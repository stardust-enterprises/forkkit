package enterprises.stardust.fukkit.modlauncher;

import cpw.mods.modlauncher.Launcher;
import dev.xdark.deencapsulation.Deencapsulation;
import org.apache.logging.log4j.LogManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

/**
 * @author xtrm
 */
class FukkitDefiner {
    private static final String[] CLASSES = {
            "cpw.mods.modlauncher.FukkitHooks",
            "cpw.mods.modlauncher.FukkitLaunchPlugin",
            "cpw.mods.modlauncher.FukkitTransformationServicesHandler",
            "cpw.mods.modlauncher.FukkitTransformingClassLoader",
    };

    private static final Object unsafeInstance;
    private static final Method defineClassMethod;

    public static void defineAll() {
        for (String className : CLASSES) {
            try {
                byte[] classBytes = getClassBytes(className);
                defineClassMethod.invoke(
                        unsafeInstance,
                        className,
                        classBytes,
                        0,
                        classBytes.length,
                        Launcher.class.getClassLoader(),
                        FukkitDefiner.class.getProtectionDomain()
                );
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        LogManager.getLogger().info("Defined all Fukkit internal classes");
    }

    private static byte[] getClassBytes(String className) {
        try {
            InputStream is = FukkitDefiner.class.getClassLoader().getResourceAsStream(
                    className.replace('.', '/') + ".class"
            );
            if (is == null) {
                throw new RuntimeException("Could not find class bytes for " + className);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            is.close();
            return baos.toByteArray();
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not read class bytes for " + className, throwable);
        }
    }

    static {
        Class<?> unsafeClass;
        try {
            unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
        } catch (Throwable throwable) {
            try {
                unsafeClass = Class.forName("sun.misc.Unsafe");
            } catch (Throwable throwable1) {
                throw new RuntimeException("Could not find Unsafe class", throwable);
            }
        }

        try {
            Deencapsulation.deencapsulate(unsafeClass);
        } catch (Throwable ignored) {
        }

        try {
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafeInstance = unsafeField.get(null);

            defineClassMethod = unsafeClass.getDeclaredMethod(
                    "defineClass",
                    String.class,
                    byte[].class,
                    int.class,
                    int.class,
                    ClassLoader.class,
                    ProtectionDomain.class
            );
            defineClassMethod.setAccessible(true);
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not get Unsafe instance", throwable);
        }
    }
}
