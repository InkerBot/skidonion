package tech.skidonion.obfuscator.transformer.generic.poly.visitors;

import tech.skidonion.obfuscator.transformer.generic.poly.model.Context;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.Add;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.Not;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.RotateLeft;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.RotateRight;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.Subtract;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.Xor;

import java.util.StringJoiner;

public class JavaVisitor extends AbstractLanguageVisitor {
    private String variable, i, result, decoded;

    @Override
    public StringBuilder initialise(Context context) {
        // Generate variable names
        variable = "temp";
        i = "i";
        result = "encoded";
        decoded = "decoded";
        // Write bytes in string
        StringBuilder sb = new StringBuilder();
        sb.append("byte[] ").append(decoded).append(" = new byte[").append(context.getEncoded().length).append("];\n");
        sb.append("int[] ").append(result).append(" = {");
        StringJoiner joiner = new StringJoiner(",");
        for (int b : context.getEncoded())
            joiner.add(String.valueOf(b));
        sb.append(joiner).append("};\n");
        // Write for loop
        sb.append(String.format("for (int %s=0, %s; %s < %s.length; %s++) {\n", i, variable, i, result, i));
        sb.append("\t" + variable + " = " + result + "[" + i + "];\n");
        return sb;
    }

    @Override
    public void finalise(StringBuilder in) {
        in.append(String.format("\t%s[%s] = (byte) (%s & 0xff);\n", decoded, i, variable))
                .append("}\n").append("System.out.println(Arrays.toString(").append(decoded).append("));");
    }

    @Override
    public void visit(Add a, StringBuilder in) {
        if (a.getValue() == 1) {
            in.append("\t").append(variable)
                    .append("++;\n");
            return;
        }
        in.append("\t").append(variable)
                .append(" += ").append(hex(a.getValue()))
                .append(";\n");
    }

    @Override
    public void visit(Not n, StringBuilder in) {
        in.append("\t").append(variable)
                .append(" = ").append("~").append(variable)
                .append(";\n");
    }

    @Override
    public void visit(RotateLeft rl, StringBuilder in) {
        in.append("\t").append(variable)
                .append(" = ").append("(").append(variable).append(" >>> ").append(hex(rl.lhs())).append(") | (").append(variable).append(" << ").append(hex(rl.rhs())).append(")")
                .append(";\n");
    }

    @Override
    public void visit(RotateRight rr, StringBuilder in) {
        in.append("\t").append(variable)
                .append(" = ").append("(").append(variable).append(" << ").append(hex(rr.lhs())).append(") | (").append(variable).append(" >>> ").append(hex(rr.rhs())).append(")")
                .append(";\n");
    }

    @Override
    public void visit(Subtract s, StringBuilder in) {
        if (s.getValue() == 1) {
            in.append("\t").append(variable)
                    .append("--;\n");
            return;
        }
        in.append("\t").append(variable)
                .append(" -= ").append(hex(s.getValue()))
                .append(";\n");
    }

    @Override
    public void visit(Xor x, StringBuilder in) {
        in.append("\t").append(variable)
                .append(" ^= ").append(hex(x.getValue()))
                .append(";\n");
    }
}
