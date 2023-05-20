package enterprises.stardust.fukkit.modlauncher;

import cpw.mods.modlauncher.FukkitTransformingClassLoader;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.net.URLClassLoader;
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

    @Override
    public @NotNull ClassNode transform(@NotNull ClassNode input, @NotNull ITransformerVotingContext context) {
        input.methods.stream().filter(it -> "<init>".equals(it.name)).forEach(it -> {
            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 'parent' param
            insnList.add(new TypeInsnNode(Opcodes.INSTANCEOF, CLASSLOADER_NAME)); // instanceof
            LabelNode label = new LabelNode();
            insnList.add(new JumpInsnNode(Opcodes.IFEQ, label)); // if instanceof
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 'parent' param
            insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, CLASSLOADER_NAME)); // cast
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CLASSLOADER_NAME, "registerChildLoader", "(Ljava/lang/ClassLoader;)V", false)); // call
            insnList.add(label);

            it.instructions.insertBefore(findLastReturn(it.instructions), insnList);
        });
        input.methods.stream().filter(it -> "loadClass0".equals(it.name)).forEach(it -> {
            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false)); // getClass()
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false)); // getClassLoader()
            insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, CLASSLOADER_NAME)); // cast
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 'name' param
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CLASSLOADER_NAME, "blockDelegation", "(Ljava/lang/String;)V", false)); // call

            it.instructions.insertBefore(it.instructions.getFirst(), insnList);
        });
        return input;
    }

    void test() {
        ((URLClassLoader) getClass().getClassLoader()).getClass();
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
