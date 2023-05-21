package cpw.mods.modlauncher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * @author xtrm
 */
public class FukkitTransformingClassLoader extends TransformingClassLoader {
    private static final Boolean DISABLE_PARALLEL =
            Boolean.getBoolean("fukkit.disableParallel");
    private static final Boolean DEBUG_LOGGING =
            Boolean.getBoolean("fukkit.debugClassLogging");
    private static final Logger LOGGER = LogManager.getLogger();
    private final Set<IClassLoaderAccess> childLoaders = new HashSet<>();
    private boolean lockDelegation = false;

    FukkitTransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, TransformingClassLoaderBuilder builder, Environment environment) {
        super(transformStore, pluginHandler, builder, environment);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (!lockDelegation) {
            for (IClassLoaderAccess loader : this.childLoaders) {
                try {
                    this.lockDelegation = true;
                    if (DEBUG_LOGGING)
                        LOGGER.info("Trying to load class from child loader: {}", loader.getClass().getName());
                    return loader.fukkit$findClassAccessor(name);
                } catch (ClassNotFoundException ignored) {
                } finally {
                    this.lockDelegation = false;
                }
            }
        }
        throw new ClassNotFoundException(name);
    }

    @SuppressWarnings("unused") // used in bytecode, see FukkitTransformer
    public Class<?> loadClassFromBukkit(String name) throws ClassNotFoundException {
        if (DEBUG_LOGGING) LOGGER.info("Loading class from Bukkit: {}", name);
        this.lockDelegation = true;
        try {
            return this.findClass(name);
        } finally {
            this.lockDelegation = false;
        }
    }

    public void registerChildLoader(IClassLoaderAccess loader) {
        if (DEBUG_LOGGING) LOGGER.info("Registering child classloader: {}", loader.getClass().getName());
        this.childLoaders.add(loader);
    }

    static {
        if (!DISABLE_PARALLEL) {
            // this *should* be fine
            ClassLoader.registerAsParallelCapable();
        } else {
            LOGGER.info("Disabling classloader parallelism");
        }
    }
}
