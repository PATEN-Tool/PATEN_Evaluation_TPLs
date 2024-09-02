// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.exportimport.dir;

import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.models.UserModel;
import java.util.List;
import org.keycloak.models.RealmModel;
import org.keycloak.models.KeycloakSession;
import java.io.IOException;
import java.io.OutputStream;
import org.keycloak.util.JsonSerialization;
import java.io.FileOutputStream;
import org.keycloak.representations.idm.RealmRepresentation;
import java.io.File;
import org.keycloak.exportimport.util.MultipleStepsExportProvider;

public class DirExportProvider extends MultipleStepsExportProvider
{
    private final File rootDirectory;
    
    public DirExportProvider() {
        final String tempDir = System.getProperty("java.io.tmpdir");
        (this.rootDirectory = new File(tempDir + "/keycloak-export")).mkdirs();
        this.logger.infof("Exporting into directory %s", (Object)this.rootDirectory.getAbsolutePath());
    }
    
    public DirExportProvider(final File rootDirectory) {
        (this.rootDirectory = rootDirectory).mkdirs();
        this.logger.infof("Exporting into directory %s", (Object)this.rootDirectory.getAbsolutePath());
    }
    
    public static boolean recursiveDeleteDir(final File dirPath) {
        if (dirPath.exists()) {
            final File[] files = dirPath.listFiles();
            for (int i = 0; i < files.length; ++i) {
                if (files[i].isDirectory()) {
                    recursiveDeleteDir(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
        return !dirPath.exists() || dirPath.delete();
    }
    
    public void writeRealm(final String fileName, final RealmRepresentation rep) throws IOException {
        final File file = new File(this.rootDirectory, fileName);
        final FileOutputStream stream = new FileOutputStream(file);
        JsonSerialization.prettyMapper.writeValue((OutputStream)stream, (Object)rep);
    }
    
    @Override
    protected void writeUsers(final String fileName, final KeycloakSession session, final RealmModel realm, final List<UserModel> users) throws IOException {
        final File file = new File(this.rootDirectory, fileName);
        final FileOutputStream os = new FileOutputStream(file);
        ExportUtils.exportUsersToStream(session, realm, users, JsonSerialization.prettyMapper, os);
    }
    
    public void close() {
    }
}
