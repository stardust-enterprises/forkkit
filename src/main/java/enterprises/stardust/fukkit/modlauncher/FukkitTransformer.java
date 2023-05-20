package enterprises.stardust.fukkit.modlauncher;

import cpw.mods.modlauncher.FukkitTransformingClassLoader;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author xtrm
 */
public class FukkitTransformer implements ITransformer<ClassNode> {
    private static final Set<Target> TARGETS = new HashSet<>();

    static {
        TARGETS.add(Target.targetClass("org/bukkit/plugin/java/PluginClassLoader"));
    }

    @Override
    public @NotNull ClassNode transform(@NotNull ClassNode input, @NotNull ITransformerVotingContext context) {
        input.methods.stream().filter(it -> "<init>".equals(it.name)).forEach(it -> {
            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 'parent' param
            insnList.add(new TypeInsnNode(Opcodes.INSTANCEOF, FukkitTransformingClassLoader.class.getName().replace('.', '/'))); // instanceof
            LabelNode label = new LabelNode();
            insnList.add(new JumpInsnNode(Opcodes.IFEQ, label)); // if instanceof
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 'parent' param
            insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, FukkitTransformingClassLoader.class.getName().replace('.', '/'))); // cast
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, FukkitTransformingClassLoader.class.getName().replace('.', '/'), "registerChildLoader", "(Ljava/lang/ClassLoader;)V", false)); // call
            insnList.add(label);

            it.instructions.insertBefore(findLastReturn(it.instructions), insnList);
        });
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
