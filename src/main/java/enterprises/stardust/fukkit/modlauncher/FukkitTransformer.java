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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

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
        MethodNode accessorMethod = new MethodNode(Opcodes.ACC_PUBLIC, IClassLoaderAccess.METHOD_NAME, IClassLoaderAccess.METHOD_DESC, IClassLoaderAccess.METHOD_SIGNATURE, new String[]{"java/lang/ClassNotFoundException"});

        InsnList accessorInsns = new InsnList();
        accessorInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        accessorInsns.add(new VarInsnNode(Opcodes.ALOAD, 1));
        accessorInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, input.name, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;", false));
        accessorInsns.add(new InsnNode(Opcodes.ARETURN));

        accessorMethod.instructions = accessorInsns;
        input.methods.add(accessorMethod);

        // Method: private Class<?> fukkit$loadFromParent(String name, boolean resolve) throws ClassNotFoundException
        String loadMethodName = "fukkit$loadFromParent";
        String loadMethodDesc = "(Ljava/lang/String;Z)Ljava/lang/Class;";
        String loadMethodSig = "(Ljava/lang/String;Z)Ljava/lang/Class<*>;";

        // lock(); try { return super.loadClass(name, resolve); } finally { unlock(); }
        MethodNode loadMethodNode = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, loadMethodName, loadMethodDesc, loadMethodSig, new String[]{"java/lang/ClassNotFoundException"});
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
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 'parent' param
            insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FUKKIT_HOOKS, "hookClassLoader", "(L" + IClassLoaderAccess.class.getName().replace('.', '/') + ";Ljava/lang/ClassLoader;)L" + CLASSLOADER_NAME + ";", false)); // FukkitHooks.hookClassLoader(this, parent)
            insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, input.name, fieldName, fieldDescriptor));

            it.instructions.insert(findLast(
                    it.instructions,
                    insn -> insn.getOpcode() == Opcodes.INVOKESTATIC
                            && ((MethodInsnNode) insn).name.equals("notNull")
            ), insnList);
        });
        input.methods.stream().filter(it -> "loadClass0".equals(it.name)).forEach(it -> {
            MethodInsnNode invokeInsn = null;
            for (AbstractInsnNode instruction : it.instructions) {
                if (instruction.getOpcode() == Opcodes.INVOKESPECIAL) {
                    invokeInsn = (MethodInsnNode) instruction;
                    break;
                }
            }
            if (invokeInsn == null) {
                throw new RuntimeException("Failed to find loadClass0 `ALOAD 0`");
            }

            it.instructions.insertBefore(
                    invokeInsn,
                    new MethodInsnNode(Opcodes.INVOKEVIRTUAL, input.name, loadMethodName, loadMethodDesc, false)
            );
            it.instructions.remove(invokeInsn);
        });

        // ClassTransformer patches
        AtomicBoolean patched = new AtomicBoolean(false);
        input.methods.stream().filter(it -> "findClass".equals(it.name) || "remappedFindClass".equals(it.name)).forEach(it -> {
            MethodInsnNode start = null;
            for (AbstractInsnNode instruction : it.instructions) {
                if (instruction.getOpcode() == Opcodes.INVOKEINTERFACE) {
                    MethodInsnNode method = (MethodInsnNode) instruction;
                    if (method.name.equals("processClass")) {
                        start = method;
                        break;
                    }
                }
            }
            if (start == null) {
                return;
            }
            patched.set(true);

            VarInsnNode first = null;
            AbstractInsnNode firstMaybe = start.getPrevious().getPrevious();
            if (firstMaybe.getOpcode() == Opcodes.ALOAD) {
                first = (VarInsnNode) firstMaybe;
            }

            if (first == null) {
                throw new RuntimeException("Failed to find `ALOAD 0` x2");
            }

            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(Opcodes.ALOAD, first.var));
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, input.name, fieldName, fieldDescriptor));
            insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FUKKIT_HOOKS, "processClass", "([BLjava/lang/String;L" + CLASSLOADER_NAME + ";)[B", false)); // FukkitHooks.hookClass(class, classLoader, this)
            it.instructions.insert(start, insnList);
        });
        if (!patched.get()) {
            throw new RuntimeException("Failed to patch ClassTransformer");
        }

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
                loadClassInsn.name = "loadClassFromBukkit";
                loadClassInsn.desc = "(Ljava/lang/String;Z)Ljava/lang/Class;";
            }
            it.instructions.insertBefore(loadClass, new InsnNode(Opcodes.ICONST_0));

            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, input.name, fieldName, fieldDescriptor));

            it.instructions.remove(start.getNext().getNext());
            it.instructions.remove(start.getNext());
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

    private static AbstractInsnNode findLast(InsnList list, Predicate<AbstractInsnNode> predicate) {
        for (int i = list.size() - 1; i >= 0; i--) {
            AbstractInsnNode insn = list.get(i);
            if (predicate.test(insn)) {
                return insn;
            }
        }
        throw new IllegalStateException("No return instruction found");
    }
}
