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
    private boolean lockDelegation = false;

    FukkitTransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, TransformingClassLoaderBuilder builder, Environment environment) {
        super(transformStore, pluginHandler, builder, environment);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            if (!lockDelegation) {
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
            }
            throw e;
        }
    }

    @SuppressWarnings("unused") // used in bytecode, see FukkitTransformer
    public void registerChildLoader(ClassLoader loader) {
        LOGGER.trace("Registering child classloader: {}", loader);
        this.childLoaders.add(loader);
    }

    @SuppressWarnings("unused") // used in bytecode, see FukkitTransformer
    public void lock() {
        this.lockDelegation = true;
    }

    @SuppressWarnings("unused") // used in bytecode, see FukkitTransformer
    public void unlock() {
        this.lockDelegation = false;
    }

    static {
        // this *should* be fine
        ClassLoader.registerAsParallelCapable();
    }
}
