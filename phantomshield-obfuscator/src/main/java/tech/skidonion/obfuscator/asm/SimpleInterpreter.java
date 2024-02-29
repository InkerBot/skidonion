package tech.skidonion.obfuscator.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

public class SimpleInterpreter extends BasicInterpreter {
    private static final BasicValue BOOLEAN_VALUE = new BasicValue(Type.BOOLEAN_TYPE);
    private static final BasicValue CHAR_VALUE = new BasicValue(Type.CHAR_TYPE);
    private static final BasicValue BYTE_VALUE = new BasicValue(Type.BYTE_TYPE);
    private static final BasicValue SHORT_VALUE = new BasicValue(Type.SHORT_TYPE);

    public SimpleInterpreter() {
        this(/* latest api = */ ASM9);
        if (getClass() != SimpleInterpreter.class) {
            throw new IllegalStateException();
        }
    }

    protected SimpleInterpreter(final int api) {
        super(api);
    }

    @Override
    public BasicValue newValue(final Type type) {
        if (type == null) {
            return BasicValue.UNINITIALIZED_VALUE;
        }

        switch (type.getSort()) {
            case Type.VOID:
                return null;
            case Type.BOOLEAN:
                return BOOLEAN_VALUE;
            case Type.CHAR:
                return CHAR_VALUE;
            case Type.BYTE:
                return BYTE_VALUE;
            case Type.SHORT:
                return SHORT_VALUE;
        }

        boolean isArray = type.getSort() == Type.ARRAY;
        if (isArray) {
            switch (type.getElementType().getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                    return new BasicValue(type);
                default:
                    break;
            }
        }

        BasicValue value = super.newValue(type);
        if (BasicValue.REFERENCE_VALUE.equals(value)) {
            if (isArray) {
                value = newValue(type.getElementType());
                StringBuilder descriptor = new StringBuilder();
                for (int i = 0; i < type.getDimensions(); ++i) {
                    descriptor.append('[');
                }
                descriptor.append(value.getType().getDescriptor());
                value = new BasicValue(Type.getType(descriptor.toString()));
            } else {
                value = new BasicValue(type);
            }
        }
        return value;
    }


    @Override
    public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.ACONST_NULL) {
            return newValue(Type.getObjectType("java/lang/Object"));
        }
        return super.newOperation(insn);
    }

    @Override
    public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.AALOAD) {
            Type arrayType = value1.getType();
            if (arrayType != null && arrayType.getSort() == Type.ARRAY) {
                return new BasicValue(arrayType.getElementType());
            }
        }
        return super.binaryOperation(insn, value1, value2);
    }


    @Override
    public BasicValue merge(BasicValue v, BasicValue w) {
        if (v.equals(w)) return v;

        if (v == BasicValue.UNINITIALIZED_VALUE || w == BasicValue.UNINITIALIZED_VALUE) {
            return BasicValue.UNINITIALIZED_VALUE;
        }

        // if merge of two references then `lub` is java/lang/Object
        // arrays also are BasicValues with reference type's
        if (isReference(v) && isReference(w)) {
            return BasicValue.REFERENCE_VALUE;
        }

        // if merge of something can be stored in int var (int, char, boolean, byte, character)
        if (v.getType().getOpcode(Opcodes.ISTORE) == Opcodes.ISTORE && w.getType().getOpcode(Opcodes.ISTORE) == Opcodes.ISTORE) {
            return BasicValue.INT_VALUE;
        }

        return BasicValue.UNINITIALIZED_VALUE;
    }

    private static boolean isReference(BasicValue v) {
        return v.getType().getSort() == Type.OBJECT || v.getType().getSort() == Type.ARRAY;
    }
}
