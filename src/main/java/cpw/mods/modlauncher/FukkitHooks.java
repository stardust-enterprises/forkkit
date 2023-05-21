package cpw.mods.modlauncher;

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

    @SuppressWarnings("unused") // Called
    public static void hookClassLoader(IClassLoaderAccess bukkitClassLoader, ClassLoader parent) {
        if (!(parent instanceof FukkitTransformingClassLoader)) {
            LOGGER.warn(
                    "{}'s parent loader is not ours, skipping... ({})",
                    bukkitClassLoader,
                    parent
            );
            return;
        }
        LOGGER.info("Adding {} as a child loader", bukkitClassLoader);
        FukkitTransformingClassLoader parentLoader = (FukkitTransformingClassLoader) parent;
        parentLoader.registerChildLoader(bukkitClassLoader);
    }

    @SuppressWarnings("unused") // Called reflectively from FukkitTransformationService
    public static void init() {
        FukkitHooks.addLaunchPlugin(new FukkitLaunchPlugin());
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
