// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.exportimport.dir;

import org.keycloak.services.managers.RealmManager;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.exportimport.util.ExportImportSessionTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.InputStream;
import org.keycloak.util.JsonSerialization;
import org.keycloak.representations.idm.RealmRepresentation;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.io.FilenameFilter;
import org.keycloak.Config;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.keycloak.exportimport.Strategy;
import org.keycloak.models.KeycloakSessionFactory;
import java.io.File;
import org.jboss.logging.Logger;
import org.keycloak.exportimport.ImportProvider;

public class DirImportProvider implements ImportProvider
{
    private static final Logger logger;
    private final File rootDirectory;
    
    public DirImportProvider() {
        final String tempDir = System.getProperty("java.io.tmpdir");
        this.rootDirectory = new File(tempDir + "/keycloak-export");
        if (!this.rootDirectory.exists()) {
            throw new IllegalStateException("Directory " + this.rootDirectory + " doesn't exists");
        }
        DirImportProvider.logger.infof("Importing from directory %s", (Object)this.rootDirectory.getAbsolutePath());
    }
    
    public DirImportProvider(final File rootDirectory) {
        this.rootDirectory = rootDirectory;
        if (!this.rootDirectory.exists()) {
            throw new IllegalStateException("Directory " + this.rootDirectory + " doesn't exists");
        }
        DirImportProvider.logger.infof("Importing from directory %s", (Object)this.rootDirectory.getAbsolutePath());
    }
    
    public void importModel(final KeycloakSessionFactory factory, final Strategy strategy) throws IOException {
        final List<String> realmNames = this.getRealmsToImport();
        for (final String realmName : realmNames) {
            this.importRealm(factory, realmName, strategy);
        }
    }
    
    public boolean isMasterRealmExported() throws IOException {
        final List<String> realmNames = this.getRealmsToImport();
        return realmNames.contains(Config.getAdminRealm());
    }
    
    private List<String> getRealmsToImport() throws IOException {
        final File[] realmFiles = this.rootDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith("-realm.json");
            }
        });
        final List<String> realmNames = new ArrayList<String>();
        for (final File file : realmFiles) {
            final String fileName = file.getName();
            final String realmName = fileName.substring(0, fileName.length() - 11);
            if (Config.getAdminRealm().equals(realmName)) {
                realmNames.add(0, realmName);
            }
            else {
                realmNames.add(realmName);
            }
        }
        return realmNames;
    }
    
    public void importRealm(final KeycloakSessionFactory factory, final String realmName, final Strategy strategy) throws IOException {
        final File realmFile = new File(this.rootDirectory + File.separator + realmName + "-realm.json");
        final File[] userFiles = this.rootDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.matches(realmName + "-users-[0-9]+\\.json");
            }
        });
        final File[] federatedUserFiles = this.rootDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.matches(realmName + "-federated-users-[0-9]+\\.json");
            }
        });
        final FileInputStream is = new FileInputStream(realmFile);
        final RealmRepresentation realmRep = (RealmRepresentation)JsonSerialization.readValue((InputStream)is, (Class)RealmRepresentation.class);
        final AtomicBoolean realmImported = new AtomicBoolean();
        KeycloakModelUtils.runJobInTransaction(factory, (KeycloakSessionTask)new ExportImportSessionTask() {
            public void runExportImportTask(final KeycloakSession session) throws IOException {
                final boolean imported = ImportUtils.importRealm(session, realmRep, strategy, true);
                realmImported.set(imported);
            }
        });
        if (realmImported.get()) {
            for (final File userFile : userFiles) {
                final FileInputStream fis = new FileInputStream(userFile);
                KeycloakModelUtils.runJobInTransaction(factory, (KeycloakSessionTask)new ExportImportSessionTask() {
                    @Override
                    protected void runExportImportTask(final KeycloakSession session) throws IOException {
                        ImportUtils.importUsersFromStream(session, realmName, JsonSerialization.mapper, fis);
                        DirImportProvider.logger.infof("Imported users from %s", (Object)userFile.getAbsolutePath());
                    }
                });
            }
            for (final File userFile : federatedUserFiles) {
                final FileInputStream fis = new FileInputStream(userFile);
                KeycloakModelUtils.runJobInTransaction(factory, (KeycloakSessionTask)new ExportImportSessionTask() {
                    @Override
                    protected void runExportImportTask(final KeycloakSession session) throws IOException {
                        ImportUtils.importFederatedUsersFromStream(session, realmName, JsonSerialization.mapper, fis);
                        DirImportProvider.logger.infof("Imported federated users from %s", (Object)userFile.getAbsolutePath());
                    }
                });
            }
        }
        KeycloakModelUtils.runJobInTransaction(factory, (KeycloakSessionTask)new ExportImportSessionTask() {
            public void runExportImportTask(final KeycloakSession session) throws IOException {
                final RealmManager realmManager = new RealmManager(session);
                realmManager.setupClientServiceAccountsAndAuthorizationOnImport(realmRep, false);
            }
        });
    }
    
    public void close() {
    }
    
    static {
        logger = Logger.getLogger((Class)DirImportProvider.class);
    }
}
