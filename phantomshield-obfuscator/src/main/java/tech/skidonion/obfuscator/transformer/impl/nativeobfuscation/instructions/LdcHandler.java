package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import tech.skidonion.obfuscator.dictionary.generator.BibleGenerator;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.verification.BufferContext;

import java.util.concurrent.ThreadLocalRandom;

public class LdcHandler extends GenericInstructionHandler<LdcInsnNode> {

    public static String getIntString(int value) {
        return value == Integer.MIN_VALUE ? "(jint) 2147483648U" : String.valueOf(value);
    }

    public static String getLongValue(long value) {
        return value == Long.MIN_VALUE ? "(jlong) 9223372036854775808ULL" : String.valueOf(value) + "LL";
    }

    public static String getFloatValue(float value) {
        if (Float.isNaN(value)) {
            return "NAN";
        } else if (value == Float.POSITIVE_INFINITY) {
            return "HUGE_VALF";
        } else if (value == Float.NEGATIVE_INFINITY) {
            return "-HUGE_VALF";
        }
        return value + "f";
    }

    public static String getDoubleValue(double value) {
        if (Double.isNaN(value)) {
            return "NAN";
        } else if (value == Double.POSITIVE_INFINITY) {
            return "HUGE_VAL";
        } else if (value == Double.NEGATIVE_INFINITY) {
            return "-HUGE_VAL";
        }
        return String.valueOf(value);
    }

    @Override
    protected void process(MethodContext context, LdcInsnNode node) {
        Object cst = node.cst;
        if (cst instanceof String) {
            instructionName += "_STRING";

            String ldc = context.getCachedStrings().getPointer(node.cst.toString());

            if (context.verificationLock != null) {
                String bible;

                if (context.bible.size() < 10) {
                    context.bible.add(bible = BibleGenerator.generate());
                } else {
                    bible = context.bible.get(ThreadLocalRandom.current().nextInt(context.bible.size()));
                }

                ldc = context.verificationLock.generateCondition(BufferContext.ConditionType.VALUE, ldc, context.getCachedStrings().getPointer(bible));
            }

            props.put("cst_ptr", ldc);

        } else if (cst instanceof Integer) {
            instructionName += "_INT";
            String ldc = getIntString((Integer) cst);
            if (context.verificationLock != null) {
                ldc = context.verificationLock.generateCondition(BufferContext.ConditionType.VALUE, ldc, getIntString(ThreadLocalRandom.current().nextInt()));
            }
            props.put("cst", ldc);
        } else if (cst instanceof Long) {
            instructionName += "_LONG";
            String ldc = getLongValue((Long) cst);
            if (context.verificationLock != null) {
                ldc = context.verificationLock.generateCondition(BufferContext.ConditionType.VALUE, ldc, getLongValue(ThreadLocalRandom.current().nextLong()));
            }
            props.put("cst", ldc);
        } else if (cst instanceof Float) {
            instructionName += "_FLOAT";
            String ldc = getFloatValue((Float) node.cst);
            if (context.verificationLock != null) {
                ldc = context.verificationLock.generateCondition(BufferContext.ConditionType.VALUE, ldc, getFloatValue(ThreadLocalRandom.current().nextFloat()));
            }
            props.put("cst", ldc);
        } else if (cst instanceof Double) {
            instructionName += "_DOUBLE";
            String ldc = getDoubleValue((Double) node.cst);
            if (context.verificationLock != null) {
                ldc = context.verificationLock.generateCondition(BufferContext.ConditionType.VALUE, ldc, getDoubleValue(ThreadLocalRandom.current().nextDouble()));
            }
            props.put("cst", ldc);
        } else if (cst instanceof Type) {
            instructionName += "_CLASS";

            int classId = context.getCachedClasses().getId(node.cst.toString());
            context.output.append(MethodProcessor.getClassCacher(context, classId, node.cst.toString(), trimmedTryCatchBlock));

            String ldc = context.getCachedClasses().getPointer(node.cst.toString());
            if (context.verificationLock != null) {
                ldc = context.verificationLock.generateCondition(BufferContext.ConditionType.VALUE, ldc, "nullptr");
            }

            props.put("cst_ptr", ldc);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String insnToString(MethodContext context, LdcInsnNode node) {
        return String.format("LDC %s", node.cst);
    }

    @Override
    public int getNewStackPointer(LdcInsnNode node, int currentStackPointer) {
        if (node.cst instanceof Double || node.cst instanceof Long) {
            return currentStackPointer + 2;
        }
        return currentStackPointer + 1;
    }
}
