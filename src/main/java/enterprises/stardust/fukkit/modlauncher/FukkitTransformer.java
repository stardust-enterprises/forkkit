package enterprises.stardust.fukkit.modlauncher;

import cpw.mods.modlauncher.FukkitTransformingClassLoader;
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
    private static final Set<Target> TARGETS = new HashSet<>();
    private static final String CLASSLOADER_NAME = FukkitTransformingClassLoader.class.getName().replace('.', '/');
    private static final String TARGET_NAME = "org/bukkit/plugin/java/PluginClassLoader";

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

        // Method: private Class<?> fukkit$loadFromParent(String name, boolean resolve) throws ClassNotFoundException
        String loadMethod = "fukkit$loadFromParent";
        String loadDescriptor = "(Ljava/lang/String;Z)Ljava/lang/Class;";
        String loadSignature = "(Ljava/lang/String;Z)Ljava/lang/Class<*>;";

        // lock(); try { return super.loadClass(name, resolve); } finally { unlock(); }
        MethodNode loadMethodNode = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, loadMethod, loadDescriptor, loadSignature, new String[]{"java/lang/ClassNotFoundException"});
        InsnList loadMethodInsns = new InsnList();

        // try/finally block
        LabelNode tryBlock = new LabelNode();
        LabelNode finallyBlock = new LabelNode();
        LabelNode catchBlock = new LabelNode();
        TryCatchBlockNode tryCatchBlockNode = new TryCatchBlockNode(tryBlock, finallyBlock, catchBlock, null);
        loadMethodNode.tryCatchBlocks.add(tryCatchBlockNode);

        // Store this.fukkit$parent in a local variable (idx: 3)
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        loadMethodInsns.add(new FieldInsnNode(Opcodes.GETFIELD, input.name, fieldName, fieldDescriptor));
        loadMethodInsns.add(new VarInsnNode(Opcodes.ASTORE, 3));

        // loader.lock();
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 3));
        loadMethodInsns.add(new JumpInsnNode(Opcodes.IFNULL, tryBlock));
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 3));
        loadMethodInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CLASSLOADER_NAME, "lock", "()V", false));

        // try block
        loadMethodInsns.add(tryBlock);
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 1));
        loadMethodInsns.add(new VarInsnNode(Opcodes.ILOAD, 2));
        loadMethodInsns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/net/URLClassLoader", "loadClass", "(Ljava/lang/String;Z)Ljava/lang/Class;", false));
        loadMethodInsns.add(new VarInsnNode(Opcodes.ASTORE, 4));

        // finally block, loader.unlock();
        loadMethodInsns.add(finallyBlock);
        LabelNode nullCheck = new LabelNode();
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 3));
        loadMethodInsns.add(new JumpInsnNode(Opcodes.IFNULL, nullCheck));
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 3));
        loadMethodInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CLASSLOADER_NAME, "unlock", "()V", false));
        // finally return
        loadMethodInsns.add(nullCheck);
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 4));
        loadMethodInsns.add(new InsnNode(Opcodes.ARETURN));

        // "catch" block
        loadMethodInsns.add(catchBlock);
        loadMethodInsns.add(new VarInsnNode(Opcodes.ASTORE, 5));
        nullCheck = new LabelNode();
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 3));
        loadMethodInsns.add(new JumpInsnNode(Opcodes.IFNULL, nullCheck));
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 3));
        loadMethodInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CLASSLOADER_NAME, "unlock", "()V", false));
        // "catch" throw
        loadMethodInsns.add(nullCheck);
        loadMethodInsns.add(new VarInsnNode(Opcodes.ALOAD, 5));
        loadMethodInsns.add(new InsnNode(Opcodes.ATHROW));

        loadMethodNode.instructions = loadMethodInsns;
        input.methods.add(loadMethodNode);

        // Patches
        input.methods.stream().filter(it -> "<init>".equals(it.name)).forEach(it -> {
            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 'parent' param
            insnList.add(new TypeInsnNode(Opcodes.INSTANCEOF, CLASSLOADER_NAME)); // instanceof
            LabelNode instanceofLabel = new LabelNode();
            insnList.add(new JumpInsnNode(Opcodes.IFEQ, instanceofLabel)); // if instanceof

            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 'parent' param
            insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, CLASSLOADER_NAME)); // cast
            insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, input.name, fieldName, fieldDescriptor)); // this.fukkit$parent = (FukkitTransformingClassLoader) parent

            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, input.name, fieldName, fieldDescriptor)); // this.fukkit$parent
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CLASSLOADER_NAME, "registerChildLoader", "(Ljava/lang/ClassLoader;)V", false)); // call
            insnList.add(instanceofLabel);

            it.instructions.insertBefore(findLastReturn(it.instructions), insnList);
        });
        input.methods.stream().filter(it -> "loadClass0".equals(it.name)).forEach(it -> {
            for (AbstractInsnNode insn : it.instructions) {
                if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                    it.instructions.insertBefore(
                            insn,
                            new MethodInsnNode(Opcodes.INVOKEVIRTUAL, input.name, loadMethod, loadDescriptor, false)
                    );
                    it.instructions.remove(insn);
                    break;
                }
            }
        });

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        input.accept(writer);
        byte[] bytes = writer.toByteArray();
        try {
            Files.write(Paths.get("PluginClassLoader.class"), bytes);
        } catch (IOException e) {
            e.printStackTrace();
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
