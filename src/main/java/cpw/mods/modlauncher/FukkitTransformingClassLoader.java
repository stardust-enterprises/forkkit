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
    private final Set<String> lookupStack = new HashSet<>();

    FukkitTransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, TransformingClassLoaderBuilder builder, Environment environment) {
        super(transformStore, pluginHandler, builder, environment);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (this.lookupStack) {
            if (this.lookupStack.contains(name)) {
                throw new ClassNotFoundException("Circular classloading dependency detected: " + name);
            }
            this.lookupStack.add(name);
            try {
                Class<?> found = super.loadClass(name, resolve);
                this.lookupStack.remove(name);
                return found;
            } catch (ClassNotFoundException e) {
                synchronized (getClassLoadingLock(name)) {
                    for (ClassLoader childLoader : this.childLoaders) {
                        try {
                            Class<?> found = childLoader.loadClass(name);
                            this.lookupStack.remove(name);
                            return found;
                        } catch (LinkageError error) {
                            error.printStackTrace();
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                }
                throw e;
            }
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
