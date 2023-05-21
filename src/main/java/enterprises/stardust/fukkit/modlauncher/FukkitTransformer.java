package enterprises.stardust.fukkit.modlauncher;

import cpw.mods.modlauncher.FukkitTransformingClassLoader;
import cpw.mods.modlauncher.IClassLoaderAccess;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * @author xtrm
 */
public class FukkitTransformer implements ITransformer<ClassNode> {
    private static final Boolean FUKKIT_DEBUG =
            Boolean.parseBoolean(System.getProperty("fukkit.debug", "false"));
    private static final Set<Target> TARGETS = new HashSet<>();

    private static final String CLASSLOADER_NAME = FukkitTransformingClassLoader.class.getName().replace('.', '/');
    private static final String TARGET_NAME = "org/bukkit/plugin/java/PluginClassLoader";
    private static final String FUKKIT_HOOKS = "cpw/mods/modlauncher/FukkitHooks";

    static {
        TARGETS.add(Target.targetClass(TARGET_NAME));
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public @NotNull ClassNode transform(@NotNull ClassNode input, @NotNull ITransformerVotingContext context) {
        // Field: private final FukkitTransformingClassLoader fukkit$parent;
        String fieldName = "fukkit$parent";
        String fieldDescriptor = "L" + CLASSLOADER_NAME + ";";
        input.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, fieldDescriptor, null, null));

        // IClassLoaderAccess
        input.interfaces.add(IClassLoaderAccess.class.getName().replace('.', '/'));
        MethodNode accessorMethod = new MethodNode(Opcodes.ACC_PUBLIC, IClassLoaderAccess.METHOD_NAME, IClassLoaderAccess.METHOD_DESC, IClassLoaderAccess.METHOD_SIGNATURE, new String[] {"java/lang/ClassNotFoundException"});

        InsnList accessorInsns = new InsnList();
        accessorInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        accessorInsns.add(new VarInsnNode(Opcodes.ALOAD, 1));
        accessorInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, input.name, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;", false));
        accessorInsns.add(new InsnNode(Opcodes.ARETURN));

        accessorMethod.instructions = accessorInsns;
        input.methods.add(accessorMethod);

        // Patches
        input.methods.stream().filter(it -> "<init>".equals(it.name)).forEach(it -> {
            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 'parent' param
            insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FUKKIT_HOOKS, "hookClassLoader", "(L" + IClassLoaderAccess.class.getName().replace('.', '/') + ";Ljava/lang/ClassLoader;)V", false)); // FukkitHooks.hookClassLoader(this, parent)

            it.instructions.insertBefore(findLastReturn(it.instructions), insnList);
        });
        // Mohist patch
        input.methods.stream().filter(it -> "findClass".equals(it.name)).forEach(it -> {
            AbstractInsnNode start = null;
            for (AbstractInsnNode instruction : it.instructions) {
                if (instruction.getOpcode() == Opcodes.INVOKESTATIC) {
                    if (instruction.getNext().getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        start = instruction;
                    }
                }
            }
            if (start == null) {
                return;
            }

            AbstractInsnNode loadClass = start.getNext().getNext().getNext().getNext();
            if (loadClass instanceof MethodInsnNode) {
                MethodInsnNode loadClassInsn = (MethodInsnNode) loadClass;
                loadClassInsn.owner = CLASSLOADER_NAME;
                loadClassInsn.name = "fromBukkit_loadClass";
            }

            it.instructions.remove(start.getNext().getNext());
            it.instructions.remove(start.getNext());
            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, input.name, fieldName, fieldDescriptor));
            it.instructions.insertBefore(start, insnList);
            it.instructions.remove(start);
        });

        if (FUKKIT_DEBUG) {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            input.accept(writer);
            byte[] bytes = writer.toByteArray();
            try {
                Files.write(Paths.get("PluginClassLoader.class"), bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return input;
    }

    @Override
    public @NotNull TransformerVoteResult castVote(@NotNull ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public @NotNull Set<Target> targets() {
        return TARGETS;
    }

    private static AbstractInsnNode findLastReturn(InsnList list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            AbstractInsnNode insn = list.get(i);
            if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN) {
                return insn;
            }
        }
        throw new IllegalStateException("No return instruction found");
    }
}
