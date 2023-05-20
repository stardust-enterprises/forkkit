package cpw.mods.modlauncher;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xtrm
 */
public class FukkitHooks {
    private static final Map<Class<?>, Logger> LOGGER_CACHE = new HashMap<>();
    private static final Class<Launcher> LAUNCHER_CLASS = Launcher.class;

    private static Logger getLogger(Class<?> clazz) {
        return LOGGER_CACHE.computeIfAbsent(clazz, LogManager::getLogger);
    }

    public static void addLaunchPlugin(ILaunchPluginService service) {
        try {
            Field field = LAUNCHER_CLASS.getDeclaredField("launchPlugins");
            field.setAccessible(true);
            LaunchPluginHandler handler = (LaunchPluginHandler) field.get(Launcher.INSTANCE);

            Field pluginsField = LaunchPluginHandler.class.getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            //noinspection unchecked
            Map<String, ILaunchPluginService> plugins = (Map<String, ILaunchPluginService>) pluginsField.get(handler);
            plugins.put(service.name(), service);

            getLogger(handler.getClass()).info(
                    "[Fukkit] Injected launch plugin {}",
                    service.name()
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void overrideTransformationServicesHandler() {
        try {
            Field field = LAUNCHER_CLASS.getDeclaredField("transformationServicesHandler");
            field.setAccessible(true);
            Object o = field.get(Launcher.INSTANCE);
            FukkitTransformationServicesHandler handler = new FukkitTransformationServicesHandler(o);
            field.set(Launcher.INSTANCE, handler);

            getLogger(FukkitTransformationServicesHandler.SUPERCLASS)
                    .info("[Fukkit] Overridden transformationServicesHandler");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // Called reflectively from FukkitTransformationService
    @SuppressWarnings("unused")
    public static void init() {
        FukkitHooks.addLaunchPlugin(new FukkitLaunchPlugin());
    }
}
