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
    private final Set<String> blockDelegation = new HashSet<>();

    FukkitTransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, TransformingClassLoaderBuilder builder, Environment environment) {
        super(transformStore, pluginHandler, builder, environment);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            synchronized (blockDelegation) {
                if (blockDelegation.contains(name)) {
                    throw e;
                }
            }
            synchronized (getClassLoadingLock(name)) {
                for (ClassLoader childLoader : this.childLoaders) {
                    try {
                        return childLoader.loadClass(name);
                    } catch (LinkageError error) {
                        error.printStackTrace();
                    } catch (Throwable ignored) {
                    }
                }
            }
            throw e;
        }
    }

    @SuppressWarnings("unused") // used in bytecode, see FukkitTransformer
    public void registerChildLoader(ClassLoader loader) {
        LOGGER.info("Registering child classloader: {}", loader);
        this.childLoaders.add(loader);
    }

    @SuppressWarnings("unused") // used in bytecode, see FukkitTransformer
    public void blockDelegation(String name) {
        synchronized (blockDelegation) {
            this.blockDelegation.add(name);
        }
    }

    static {
        // this *should* be fine
        ClassLoader.registerAsParallelCapable();
    }
}
