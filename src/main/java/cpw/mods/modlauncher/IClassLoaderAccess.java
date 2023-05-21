package cpw.mods.modlauncher;

/**
 * @author xtrm
 */
@FunctionalInterface
public interface IClassLoaderAccess {
    String METHOD_NAME = "fukkit$findClassAccessor";
    String METHOD_DESC = "(Ljava/lang/String;)Ljava/lang/Class;";
    String METHOD_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/Class<*>;";

    @SuppressWarnings("unuseld") // used in bytecode, see FukkitTransformer
    Class<?> fukkit$findClassAccessor(String name) throws ClassNotFoundException;
}
