package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformerActivity;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author xtrm
 */
public class FukkitHooks {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final boolean IS_MOHIST;

    @SuppressWarnings("unused") // Called reflectively from FukkitTransformationService
    public static void init() {
        FukkitHooks.addLaunchPlugin(new FukkitLaunchPlugin());
    }

    public static void addLaunchPlugin(ILaunchPluginService service) {
        try {
            Field field = Launcher.class.getDeclaredField("launchPlugins");
            field.setAccessible(true);
            LaunchPluginHandler handler = (LaunchPluginHandler) field.get(Launcher.INSTANCE);

            Field pluginsField = LaunchPluginHandler.class.getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            //noinspection unchecked
            Map<String, ILaunchPluginService> plugins =
                    (Map<String, ILaunchPluginService>) pluginsField.get(handler);
            plugins.put(service.name(), service);

            LOGGER.info("Injected launch plugin {}", service.name());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void overrideTransformationServicesHandler() {
        try {
            Field field = Launcher.class.getDeclaredField("transformationServicesHandler");
            field.setAccessible(true);
            field.set(
                    Launcher.INSTANCE,
                    new FukkitTransformationServicesHandler(
                            field.get(Launcher.INSTANCE)
                    )
            );

            LOGGER.info("Overridden transformationServicesHandler");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused") // Called in bytecode, see FukkitTransformer
    public static FukkitTransformingClassLoader hookClassLoader(IClassLoaderAccess bukkitClassLoader, ClassLoader parent) {
        if (!(parent instanceof FukkitTransformingClassLoader)) {
            throw new RuntimeException("Parent classloader is not a FukkitTransformingClassLoader");
        }
        FukkitTransformingClassLoader parentLoader = (FukkitTransformingClassLoader) parent;
        parentLoader.registerChildLoader(bukkitClassLoader);
        return parentLoader;
    }

    @SuppressWarnings("unused") // Called in bytecode, see FukkitTransformer
    public static byte[] processClass(
            byte[] classBytes,
            String path,
            FukkitTransformingClassLoader ftcl
    ) {
        String className = path.substring(0, path.length() - 6).replace('/', '.');
        boolean swap = ftcl.isLocked();
        if (swap) {
            ftcl.unlock();
        }
        try {
            return ftcl.getClassTransformer().transform(classBytes, className, ITransformerActivity.COMPUTING_FRAMES_REASON);
        } finally {
            if (swap) {
                ftcl.lock();
            }
        }
    }

    static {
        boolean isMohist = false;
        try {
            Class.forName("com.mohistmc.MohistMC");
            isMohist = true;
            LOGGER.info("Mohist detected.");
        } catch (ClassNotFoundException ignored) {
        }
        IS_MOHIST = isMohist;
    }
}
