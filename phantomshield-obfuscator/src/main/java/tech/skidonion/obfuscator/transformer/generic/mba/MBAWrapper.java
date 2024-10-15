package tech.skidonion.obfuscator.transformer.generic.mba;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import tech.skidonion.obfuscator.mba.LinearMBA;
import tech.skidonion.obfuscator.mba.expr.Expr;
import tech.skidonion.obfuscator.mba.expr.ExprOp;
import tech.skidonion.obfuscator.mba.expr.operations.*;
import tech.skidonion.obfuscator.mba.helper.Valuation;
import tech.skidonion.obfuscator.mba.obfuscate.ObfuscationConfig;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.commons.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MBAWrapper implements Opcodes {
    private int methodLocalsIndex = -1;
    private final Expr expr;
    private final HashMap<String, MBAValue> kv = new HashMap<>();
    private Valuation.MissingValue missingValueHandler = Valuation.MissingValue.zero();

    private MBAWrapper(Expr expr) {
        this.expr = expr;
    }

    private MBAWrapper(Expr originalExpr, ObfuscationConfig cfg) {
        LinearMBA.obfuscate(originalExpr, 32, cfg);
        this.expr = originalExpr;
    }

    public static MBAWrapper wrap(Expr expr) {
        return new MBAWrapper(expr);
    }

    /**
     * @param expr it will rewrite the expr, so you may be unable to use the expr again.
     */
    public static MBAWrapper obfuscate(Expr expr, ObfuscationConfig cfg) {
        return new MBAWrapper(expr, cfg);
    }

    public void set(String var, MBAValue value) {
        this.kv.put(var, value);
    }

    /**
     * @param methodLocalsIndex you may use ASMUtils.computeMaxLocals(MethodNode)
     */
    public void setMethodLocalsIndex(int methodLocalsIndex) {
        this.methodLocalsIndex = methodLocalsIndex;
    }

    public void setMissingValueHandler(Valuation.MissingValue missingValueHandler) {
        this.missingValueHandler = missingValueHandler;
    }

    /**
     * @return return the expression's vars.
     */
    public ArrayList<String> vars() {
        return this.expr.getOp().vars();
    }

    public InsnList generate() {
        HashMap<ExprOp, Pair<Integer, InsnList>> vars = new HashMap<>();
        InsnList __ = new InsnList();
        InsnList generated = new InsnList();
        generated.add(generateImpl(this.expr, vars, false));
        for (Pair<Integer, InsnList> pair : vars.values()) {
            __.add(pair.getSecond());
            __.add(new VarInsnNode(ISTORE, pair.getFirst()));
        }
        __.add(generated);
        return __;
    }

    private InsnList generateImpl(Expr e, HashMap<ExprOp, Pair<Integer, InsnList>> vars, boolean generateLocal) {
        Expr multiRef;
        if (e.getOp().referencedSize() > 1 && !generateLocal) {
            multiRef = e;
            if (methodLocalsIndex == -1) {
                throw new RuntimeException("An extra local is generated, but didn't provide a new local index.");
            } else if (!vars.containsKey(e.getOp())) {
                vars.put(e.getOp(), new Pair<>(this.methodLocalsIndex++, generateImpl(e, vars, true)));
            }
            e = ExprOp.var("");
        } else {
            multiRef = null;
        }
        InsnList __ = new InsnList();
        switch (e.getOp().type()) {
            case Const: {
                __.add(new LdcInsnNode(((Const) e.getOp()).getVal().intValue()));
                break;
            }
            case Var: {
                Var m = (Var) e.getOp();
                if (multiRef != null) {
                    __.add(new VarInsnNode(ILOAD, vars.get(multiRef.getOp()).getFirst()));
                } else {
                    MBAValue val = this.kv.get(m.getVar());
                    if (val == null) {
                        __.add(ASMUtils.getNumberInsn(missingValueHandler.handleMissing(m.getVar()).intValue()));
                    } else if (val.getType() == MBAValue.MBAValueType.LOCAL) {
                        __.add(new VarInsnNode(ILOAD, val.getValue()));
                    } else {
                        __.add(ASMUtils.getNumberInsn(val.getValue()));
                    }
                }
                break;
            }
            case Add: {
                Add m = (Add) e.getOp();
                __.add(generateImpl(m.getLeft(), vars, false));
                __.add(generateImpl(m.getRight(), vars, false));
                __.add(new InsnNode(IADD));
                break;
            }
            case Sub: {
                Sub m = (Sub) e.getOp();
                __.add(generateImpl(m.getLeft(), vars, false));
                __.add(generateImpl(m.getRight(), vars, false));
                __.add(new InsnNode(ISUB));
                break;
            }
            case Mul: {
                Mul m = (Mul) e.getOp();
                __.add(generateImpl(m.getLeft(), vars, false));
                __.add(generateImpl(m.getRight(), vars, false));
                __.add(new InsnNode(IMUL));
                break;
            }
            case Div: {
                Div m = (Div) e.getOp();
                __.add(generateImpl(m.getLeft(), vars, false));
                __.add(generateImpl(m.getRight(), vars, false));
                __.add(new InsnNode(IDIV));
                break;
            }
            case Neg: {
                Neg m = (Neg) e.getOp();
                __.add(generateImpl(m.getExpr(), vars, false));
                __.add(new InsnNode(INEG));
                break;
            }
            case And: {
                And m = (And) e.getOp();
                __.add(generateImpl(m.getLeft(), vars, false));
                __.add(generateImpl(m.getRight(), vars, false));
                __.add(new InsnNode(IAND));
                break;
            }
            case Or: {
                Or m = (Or) e.getOp();
                __.add(generateImpl(m.getLeft(), vars, false));
                __.add(generateImpl(m.getRight(), vars, false));
                __.add(new InsnNode(IOR));
                break;
            }
            case Xor: {
                Xor m = (Xor) e.getOp();
                __.add(generateImpl(m.getLeft(), vars, false));
                __.add(generateImpl(m.getRight(), vars, false));
                __.add(new InsnNode(IXOR));
                break;
            }
            case Not: {
                Not m = (Not) e.getOp();
                __.add(generateImpl(m.getExpr(), vars, false));
                __.add(new InsnNode(ICONST_M1));
                __.add(new InsnNode(IXOR));
                break;
            }
            case Shl: {
                Shl m = (Shl) e.getOp();
                __.add(generateImpl(m.getLeft(), vars, false));
                __.add(generateImpl(m.getRight(), vars, false));
                __.add(new InsnNode(ISHL));
                break;
            }
            case Shr: {
                Shr m = (Shr) e.getOp();
                __.add(generateImpl(m.getLeft(), vars, false));
                __.add(generateImpl(m.getRight(), vars, false));
                __.add(new InsnNode(ISHR));
                break;
            }
            case Sar: {
                Sar m = (Sar) e.getOp();
                __.add(generateImpl(m.getLeft(), vars, false));
                __.add(generateImpl(m.getRight(), vars, false));
                __.add(new InsnNode(IUSHR));
                break;
            }
            default: {
                throw new RuntimeException("unknown op: " + e.getOp());
            }
        }
        return __;
    }
}
