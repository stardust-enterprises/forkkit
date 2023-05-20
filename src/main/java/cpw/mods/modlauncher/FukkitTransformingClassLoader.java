package cpw.mods.modlauncher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * @author xtrm
 */
public class FukkitTransformingClassLoader extends TransformingClassLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Set<ClassLoader> childLoaders = new HashSet<>();

    FukkitTransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, TransformingClassLoaderBuilder builder, Environment environment) {
        super(transformStore, pluginHandler, builder, environment);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            synchronized (getClassLoadingLock(name)) {
                for (ClassLoader childLoader : this.childLoaders) {
                    try {
                        return childLoader.loadClass(name);
                    } catch (LinkageError error) {
                        error.printStackTrace();
                    } catch (ClassNotFoundException ignored) {
                    }
                }
            }
            throw e;
        }
    }

    public void registerChildLoader(ClassLoader loader) {
        LOGGER.info("Registering child classloader: {}", loader);
        this.childLoaders.add(loader);
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}
