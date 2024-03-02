package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.generic.CodeBlock;
import tech.skidonion.obfuscator.transformer.generic.ResolvedBlocks;
import tech.skidonion.obfuscator.transformer.generic.StackCodeBlockMap;
import tech.skidonion.obfuscator.transformer.generic.TryCatchBlock;
import tech.skidonion.obfuscator.transformer.generic.resolver.CodeBlockResolver;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.RandomUtils;

import java.util.*;

import static tech.skidonion.obfuscator.PhantomShield.INFO;

public class ControlFlowObfuscation extends Transformer implements Opcodes {
    public ControlFlowObfuscation(String name) {
        super(name);
    }

    @Override
    public void transform() throws Exception {
        getFilteredClasses().forEach(cw -> cw.getMethods().stream().filter(wrapper -> wrapper.getInstructions().size() > 0 && this.match(wrapper)).forEach(wrapper -> {
            MethodNode method = wrapper.getMethodNode();
            // TODO: ignore init??
            if (method.name.equals("<init>")) {
                return;
            }
            // delete the fuck shit
            method.localVariables = null;
            ResolvedBlocks resolved = CodeBlockResolver.resolve(method);

            // add opaque predications
            List<CodeBlock> generatedBlocks = new ArrayList<>();
            this.addOpaquePredicate(resolved, generatedBlocks);
            resolved.getResolvedBlocks().addAll(generatedBlocks);

            // shuffle labels orders
            InsnList shuffled = new InsnList();
            shuffled.add(new LabelNode());

            // initialization all variables
            int locals = (ASMUtils.getFlag(method.access, ACC_STATIC) ? 0 : 1);
            for (Type type : Type.getArgumentTypes(method.desc)) {
                locals += type.getSize();
            }
            for (int index = locals; index < resolved.getLocals().size(); index++) {
                Type type = resolved.getLocals().get(index);
                if (type != null) {
                    shuffled.add(generateDefaultValue(type));
                    shuffled.add(new VarInsnNode(ASMUtils.getVarOpcode(type, true), index));
                }
            }

            // goto the entry point
            shuffled.add(new JumpInsnNode(GOTO, resolved.getResolvedBlocks().getFirst().getLabel()));
            shuffle(resolved);
            shuffled.add(resolved.toInsnList());

            // add a default return value or will loop while compute max stacks/locals
            Type returnType = Type.getReturnType(method.desc);
            int opcode = ASMUtils.getReturnOpcode(returnType);
            shuffled.add(new LabelNode());
            if (opcode != RETURN) shuffled.add(ASMUtils.getDefaultValue(returnType));
            shuffled.add(new InsnNode(opcode));

            method.instructions = shuffled;
        }));
    }

    @Override
    public void preprocess() throws Exception {
    }

    @Override
    public String annotation() {
        return null;
    }

    private void shuffle(ResolvedBlocks resolved) {
        for (CodeBlock resolvedBlock : resolved.getResolvedBlocks()) {
            if (resolvedBlock instanceof TryCatchBlock) {
                shuffle((TryCatchBlock) resolvedBlock);
                continue;
            }
        }
        ArrayList<CodeBlock> clone = new ArrayList<>(resolved.getResolvedBlocks());
        Collections.shuffle(clone);
        resolved.setResolvedBlocks(new LinkedList<>(clone));
    }

    private void shuffle(TryCatchBlock tryCatchBlock) {
        LinkedList<CodeBlock> codes = tryCatchBlock.getCodes();
        CodeBlock endBlock = tryCatchBlock.getEndBlock();

        LinkedList<CodeBlock> shuffled = new LinkedList<>();
        if (codes.size() > 2) {
            for (CodeBlock code : codes) {
                if (code instanceof TryCatchBlock) {
                    shuffle((TryCatchBlock) code);
                    continue;
                }
            }
            shuffled.add(codes.getFirst());
            codes.remove();
            ArrayList<CodeBlock> clone = new ArrayList<>(codes);
            Collections.shuffle(clone);
            shuffled.addAll(clone);
            tryCatchBlock.setCodes(shuffled);
        }

        CodeBlock next = endBlock.getNext();
        if (next != null) {
            endBlock.getInstructions().add(new JumpInsnNode(GOTO, next.getLabel()));
        }
    }

    private void addOpaquePredicate(ResolvedBlocks resolved, List<CodeBlock> generatedBlocks) {
        Collections.shuffle(resolved.getClonedList());
        for (CodeBlock code : resolved.getResolvedBlocks()) {
            if (code instanceof TryCatchBlock) {
                addOpaquePredicate(resolved, (TryCatchBlock) code, generatedBlocks);
                continue;
            }
            addOpaquePredicate(resolved, code, resolved.getStackCodeBlockMap(), generatedBlocks);
        }
    }

    private void addOpaquePredicate(ResolvedBlocks resolved, TryCatchBlock tryCatchBlock, List<CodeBlock> generatedBlocks) {
        Collections.shuffle(tryCatchBlock.getClonedList());
        for (CodeBlock code : tryCatchBlock.getCodes()) {
            if (code instanceof TryCatchBlock) {
                addOpaquePredicate(resolved, (TryCatchBlock) code, generatedBlocks);
                continue;
            }
            addOpaquePredicate(resolved, code, tryCatchBlock.getStackCodeBlockMap(), generatedBlocks);
        }
    }

    private void addOpaquePredicate(ResolvedBlocks resolved, CodeBlock code, StackCodeBlockMap stacks, List<CodeBlock> generatedBlocks) {
        InsnList insns = code.getInstructions();
        CodeBlock next = code.getNext();
        AbstractInsnNode insn = code.getInstructions().getLast();
        // condition: next code block will available to ensure correct order
        // make sure the last instruction is force jump or return
        // TODO: ASMUtils.isJumpOrReturnOpcode is not necessary.
        //  we can use the return or jump opcode to make fake jump
        //  it can be more complex to decompile/analyze
        if (next != null && insn != null && !ASMUtils.isJumpOrReturnOpcode(insn.getOpcode())) {
            // generate fake jump
            Frame<BasicValue> currentFrame = next.getFrame(0);
            Type[] types = new Type[currentFrame.getStackSize()];
            for (int i = 0; i < currentFrame.getStackSize(); i++) {
                types[i] = currentFrame.getStack(i).getType();
            }

            List<CodeBlock> stackLabels = stacks.get(new StackCodeBlockMap.Stack(types));
            if (stackLabels == null) {
                insns.add(new JumpInsnNode(GOTO, next.getLabel()));
                return;
            }
            CodeBlock magicBlock = stackLabels.get(RandomUtils.getRandomInt(stackLabels.size()));
            LabelNode magic = magicBlock.getLabel();

            Frame<BasicValue> magicFrame = magicBlock.getFrame(0);

            // if the random code block is unreachable code
            // we should make its frame keep empty instead of "null"
            if (magicFrame == null) {
                magicFrame = new Frame<>(currentFrame.getLocals(), currentFrame.getMaxStackSize());
            }
            if (currentFrame != null) {
                // balance frame stack map
                LabelNode balancedLabel = new LabelNode();
                CodeBlock balancedBlock = new CodeBlock(balancedLabel);
                InsnList balanced = new InsnList();
                balanced.add(balancedLabel);

                int currentStackSize = currentFrame.getStackSize();
                int magicStackSize = magicFrame.getStackSize();

                int covered = Math.min(magicStackSize, currentStackSize);

                // make sure the stack map is always the same
                boolean clean = false;

                for (int i = 0; i < covered; i++) {
                    Type currentValueSort = currentFrame.getStack(i).getType();
                    Type magicValueSort = magicFrame.getStack(i).getType();
                    if (!currentValueSort.toString().equals(magicValueSort.toString())) {
                        clean = true;
                        break;
                    }
                }
                if (clean) {
                    for (int i = 0; i < currentStackSize; i++) {
                        Type type = currentFrame.getStack(currentStackSize - 1 - i).getType();
                        balanced.add(generateDummyPop(type));
                    }
                    currentFrame = new Frame<>(currentFrame.getLocals(), currentFrame.getMaxStackSize());
                    currentStackSize = currentFrame.getStackSize();
                }

                if (currentStackSize > magicStackSize) {
                    int l = currentStackSize - magicStackSize;
                    for (int i = 0; i < l; i++) {
                        Type type = currentFrame.getStack(currentStackSize - 1 - i).getType();
                        balanced.add(generateDummyPop(type));
                    }
                } else if (currentStackSize < magicStackSize) {
                    int l = magicStackSize - currentStackSize;
                    for (int i = 0; i < l; i++) {
                        BasicValue value = magicFrame.getStack(currentStackSize + i);
                        balanced.add(generateDefaultValue(value.getType()));
                    }
                }

                balanced.add(new JumpInsnNode(GOTO, magic));
                balancedBlock.setInstructions(balanced);

                LabelNode expected;

                // if we processed nothing
                // we should make a direct goto
                if (balanced.size() > 2) {
                    generatedBlocks.add(balancedBlock);
                    expected = balancedLabel;
                } else {
                    expected = magic;
                }


                // make sure that it will always jump to the correct case
                boolean generate = RandomUtils.getRandomBoolean();
                boolean if_equals = RandomUtils.getRandomBoolean();
//                boolean generate = true;
//                boolean if_equals = false;
                LabelNode label1;
                LabelNode label2;
                if ((generate && if_equals) || (!generate && !if_equals)) {
                    label1 = next.getLabel();
                    label2 = expected;
                } else {
                    label2 = next.getLabel();
                    label1 = expected;
                }


                // TODO: the fuck opaque predications
                // =======
                insns.add(generate ? ASMUtils.generateFalse() /* iconst_0 */ : ASMUtils.generateTrue() /* iconst_1 */);
//                insns.add(generate ? new InsnNode(ICONST_0) : new InsnNode(ICONST_1));
                // =======


                insns.add(new JumpInsnNode(if_equals ? IFEQ : IFNE, label1));
                insns.add(new JumpInsnNode(GOTO, label2));
            } else {
                return; // unreachable code
            }
        }
    }

    private static final Map<String, Integer> ARRAY_TYPES = new HashMap<String, Integer>() {
        {
            put("[I", Opcodes.T_INT);
            put("[Z", Opcodes.T_BOOLEAN);
            put("[C", Opcodes.T_CHAR);
            put("[F", Opcodes.T_FLOAT);
            put("[B", Opcodes.T_BYTE);
            put("[D", Opcodes.T_DOUBLE);
            put("[S", Opcodes.T_SHORT);
            put("[J", Opcodes.T_LONG);
        }
    };

    private static InsnList generateDefaultValue(Type type) {
        int sort = type.getSort();
        InsnList insns = new InsnList();
        if (sort == Type.ARRAY) {
            String typeInternalName = type.getInternalName();
            Integer arrayType = ARRAY_TYPES.get(typeInternalName);
            if (arrayType != null) {
                insns.add(new InsnNode(ICONST_0));
                insns.add(new IntInsnNode(Opcodes.NEWARRAY, arrayType));
            } else {
                insns.add(new InsnNode(ACONST_NULL));
            }
        } else {
            insns.add(ASMUtils.getDefaultValue(type));
        }
        return insns;
    }

    private static MethodInsnNode generateDummyPop(Type type) {
        String owner = "a" + RandomUtils.getRandomInt();
        String name = "a" + RandomUtils.getRandomInt();
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return new MethodInsnNode(INVOKESTATIC, owner, name, "(Z)V");
            case Type.CHAR:
                return new MethodInsnNode(INVOKESTATIC, owner, name, "(C)V");
            case Type.BYTE:
                return new MethodInsnNode(INVOKESTATIC, owner, name, "(B)V");
            case Type.SHORT:
                return new MethodInsnNode(INVOKESTATIC, owner, name, "(S)V");
            case Type.INT:
                return new MethodInsnNode(INVOKESTATIC, owner, name, "(I)V");
            case Type.FLOAT:
                return new MethodInsnNode(INVOKESTATIC, owner, name, "(F)V");
            case Type.LONG:
                return new MethodInsnNode(INVOKESTATIC, owner, name, "(J)V");
            case Type.DOUBLE:
                return new MethodInsnNode(INVOKESTATIC, owner, name, "(D)V");
            case Type.ARRAY:
            case Type.OBJECT:
                return new MethodInsnNode(INVOKESTATIC, owner, name, "(" + type.getDescriptor() + ")V");
            default:
                throw new RuntimeException("Can't generate dummy pop insn");
        }
    }
}
