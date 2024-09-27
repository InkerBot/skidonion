package tech.skidonion.obfuscator.transformer.generic.poly.visitors;

import tech.skidonion.obfuscator.transformer.generic.poly.model.Context;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.Add;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.Not;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.RotateLeft;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.RotateRight;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.Subtract;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.Xor;

public class CVisitor extends AbstractLanguageVisitor {
    protected String variable, decoded, i, result;

    @Override
    public StringBuilder initialise(Context context) {
        // Generate variable names
        variable = "temp";
        i = "i";
        result = "encoded";
        decoded = "decoded";
        // Write bytes in string
        StringBuilder sb = new StringBuilder();
        int[] bytes = context.getEncoded();
        sb.append("char ").append(decoded).append("[").append(bytes.length + 1).append("] = {0};\n");
        sb.append("unsigned int " + result  + "[" + bytes.length + "] = {");
        for (int b : bytes)
            sb.append(hex(b) + ",");
        sb.deleteCharAt(sb.length() - 1)    // remove last comma
                .append("};\n");
        // Write for loop
        sb.append(String.format("for (unsigned int %s=0, %s; %s < %d; %s++) {\n", i, variable, i, bytes.length, i));
        sb.append("\t" + variable + " = " + result + "[" + i + "];\n");
        return sb;
    }

    @Override
    public void finalise(StringBuilder in) {
        in.append(String.format("\t%s[%s] = %s & 0xff;\n", decoded, i, variable))
                .append("}\n")
                .append("printf(" + decoded + ");");
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
                .append(" = ").append("~" + variable)
                .append(";\n");
    }

    @Override
    public void visit(RotateLeft rl, StringBuilder in) {
        in.append("\t").append(variable)
                .append(" = ").append(variable + " >> " + hex(rl.lhs()) + " | ")
                .append(variable + " << " + hex(rl.rhs()))
                .append(";\n");
    }

    @Override
    public void visit(RotateRight rr, StringBuilder in) {
        in.append("\t").append(variable)
                .append(" = ").append(variable + " << " + hex(rr.lhs()) + " | ")
                .append(variable + " >> " + hex(rr.rhs()) )
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
