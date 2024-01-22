package enterprises.stardust.fukkit.modlauncher;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author xtrm
 */
@SuppressWarnings("rawtypes")
public class FukkitTransformationService implements ITransformationService {
    private static final Boolean FUKKIT_DEBUG =
            Boolean.parseBoolean(System.getProperty("fukkit.debug", "false"));
    public static final List<ITransformer> TRANSFORMERS =
            Collections.singletonList(new FukkitTransformer());

    static {
        Logger log = LogManager.getLogger();
        log.info("Hello Fukkit modlauncher :3");

        try {
            FukkitDefiner.defineAll();
            Class<?> clazz = Launcher.class.getClassLoader().loadClass("cpw.mods.modlauncher.FukkitHooks");
            clazz.getMethod("init").invoke(null);
            log.trace("Initialized FukkitHooks");
        } catch (Throwable e) {
            log.error("Failed to initialize FukkitHooks", e);
        }
    }

    @Override
    public @NotNull String name() {
        return "fukkit";
    }

    @Override
    public void initialize(@NotNull IEnvironment environment) {
        if (FUKKIT_DEBUG) {
            ClassLoader loader = getClass().getClassLoader();
            int i = 0;
            do {
                String padding = new String(new char[i]).replace('\0', ' ');
                String name;
                if (loader == null) {
                    name = "(builtin)";
                } else {
                    name = loader.getClass().getName() + " (" + Integer.toHexString(loader.getClass().hashCode()) + ")";
                    loader = loader.getParent();
                }
                System.out.println(padding + "- " + name);
                i++;
            } while (loader != null);
        }
    }

    @Override
    public void beginScanning(@NotNull IEnvironment environment) {
    }

    @Override
    public void onLoad(@NotNull IEnvironment env, @NotNull Set<String> otherServices) {
    }

    @Override
    public @NotNull List<ITransformer> transformers() {
        return TRANSFORMERS;
    }
}
