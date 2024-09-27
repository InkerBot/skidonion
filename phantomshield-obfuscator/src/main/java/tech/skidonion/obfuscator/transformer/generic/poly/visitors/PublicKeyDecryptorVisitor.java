package tech.skidonion.obfuscator.transformer.generic.poly.visitors;

import tech.skidonion.obfuscator.transformer.generic.poly.model.Context;

public class PublicKeyDecryptorVisitor extends CVisitor {

    @Override
    public StringBuilder initialise(Context context) {
        variable = "temp";
        i = "i";
        result = "i_encoded_public_key";
        decoded = "public_key";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("for (unsigned int %s=0, %s; %s < %d; %s++) {\n", i, variable, i, 32, i));
        sb.append("\t" + variable + " = " + result + "[" + i + "];\n");
        return sb;
    }

    @Override
    public void finalise(StringBuilder in) {
        in.append(String.format("\t%s[%s] = static_cast<unsigned char>(%s & 0xff);\n", decoded, i, variable)).append("}\n");
    }
}
