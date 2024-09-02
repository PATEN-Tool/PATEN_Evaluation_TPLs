// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.commons.configuration2;

import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.representer.Representer;
import java.io.InputStream;
import java.io.IOException;
import org.yaml.snakeyaml.DumperOptions;
import java.io.Writer;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import java.io.Reader;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.io.InputStreamSupport;

public class YAMLConfiguration extends AbstractYAMLBasedConfiguration implements FileBasedConfiguration, InputStreamSupport
{
    public YAMLConfiguration() {
    }
    
    public YAMLConfiguration(final HierarchicalConfiguration<ImmutableNode> c) {
        super(c);
    }
    
    @Override
    public void read(final Reader in) throws ConfigurationException {
        try {
            final Yaml yaml = createYamlForReading(new LoaderOptions());
            final Map<String, Object> map = (Map<String, Object>)yaml.load(in);
            this.load(map);
        }
        catch (Exception e) {
            AbstractYAMLBasedConfiguration.rethrowException(e);
        }
    }
    
    public void read(final Reader in, final LoaderOptions options) throws ConfigurationException {
        try {
            final Yaml yaml = createYamlForReading(options);
            final Map<String, Object> map = (Map<String, Object>)yaml.load(in);
            this.load(map);
        }
        catch (Exception e) {
            AbstractYAMLBasedConfiguration.rethrowException(e);
        }
    }
    
    @Override
    public void write(final Writer out) throws ConfigurationException, IOException {
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.dump(out, options);
    }
    
    public void dump(final Writer out, final DumperOptions options) throws ConfigurationException, IOException {
        final Yaml yaml = new Yaml(options);
        yaml.dump((Object)this.constructMap(this.getNodeModel().getNodeHandler().getRootNode()), out);
    }
    
    @Override
    public void read(final InputStream in) throws ConfigurationException {
        try {
            final Yaml yaml = createYamlForReading(new LoaderOptions());
            final Map<String, Object> map = (Map<String, Object>)yaml.load(in);
            this.load(map);
        }
        catch (Exception e) {
            AbstractYAMLBasedConfiguration.rethrowException(e);
        }
    }
    
    public void read(final InputStream in, final LoaderOptions options) throws ConfigurationException {
        try {
            final Yaml yaml = createYamlForReading(options);
            final Map<String, Object> map = (Map<String, Object>)yaml.load(in);
            this.load(map);
        }
        catch (Exception e) {
            AbstractYAMLBasedConfiguration.rethrowException(e);
        }
    }
    
    private static Yaml createYamlForReading(final LoaderOptions options) {
        return new Yaml((BaseConstructor)createClassLoadingDisablingConstructor(), new Representer(), new DumperOptions(), options);
    }
    
    private static Constructor createClassLoadingDisablingConstructor() {
        return new Constructor() {
            protected Class<?> getClassForName(final String name) {
                throw new ConfigurationRuntimeException("Class instantiation is disabled.");
            }
        };
    }
}
