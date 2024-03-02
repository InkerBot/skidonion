package tech.skidonion.obfuscator.transformer.addon;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.crypto.ChaCha20;
import tech.skidonion.obfuscator.utils.RandomUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Watermarking extends Addon {
    private final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
    private final String date = formatter.format(new Date());

    @Override
    public void transform() throws Exception {
        byte[] publicKey = new byte[]{1, 0, 1};
        byte[] modules = new byte[]{0, -99, 82, 5, -109, -69, -32, 51, 126, 67, 127, 125, 30, -97, -86, -59, 81, 4, 3, 50, 15, 14, 61, -89, -46, 105, -102, 51, -118, 99, -22, 109, -93, 87, -82, -84, 57, -90, 106, -73, 20, -33, -42, -104, -30, -70, 4, 8, 39, -100, 106, 80, -60, 108, -52, -120, 94, 111, 9, -116, -57, -80, 110, -66, -47, 29, 59, 87, -117, 24, -5, 97, -66, 46, 90, -10, -34, 120, -12, -42, 43, 80, -99, 27, 33, -12, 13, 64, 20, 24, -28, 106, -71, -77, 43, 48, -7, -59, -125, 105, 13, 2, 56, -40, 103, 74, 100, 118, -39, -76, 46, -29, 86, 111, -45, -27, 25, -58, -33, 73, 54, -4, 21, -96, -112, -78, 51, 33, -10, -54, -111, -103, -15, -55, 110, -66, -127, -26, -122, 107, 44, 87, -66, 57, 29, 34, -53, 26, -89, -74, -68, -11, 81, 96, -114, 44, -72, -32, -101, 83, 22, -18, -115, 114, -86, 127, 0, -39, 63, 104, -53, -128, -4, -74, -127, -93, 98, -96, 33, -126, -75, 30, 16, -49, 26, 106, 49, -85, 74, -81, 10, -84, -13, -29, 96, -41, 120, -59, 37, 37, 111, -48, 33, -55, -35, -49, -33, 2, -117, -48, -6, -50, -63, -90, 3, -127, -85, 108, -71, -76, 17, -109, -6, -26, 60, -101, -65, 12, -9, -111, 35, 15, -45, 110, 11, 125, 30, -60, 99, -90, 77, 57, -128, 113, 11, -33, 25, -84, 20, -111, -5, 62, -118, 19, 39, 38, -37};

        List<ClassWrapper> wrappers = new ArrayList<>(getClassWrappers());
        Set<ClassWrapper> injected = new HashSet<>();
        for (int times = 0; times < 3; times++) {
            ClassWrapper cw = wrappers.get(RandomUtils.getRandomInt(wrappers.size()));
            if (!injected.add(cw)) continue;
            MethodNode method = findOrCreateMethodWrapper(cw);
            String magic = new String(createMagicInformation(publicKey, modules), StandardCharsets.ISO_8859_1);

            InsnList __ = new InsnList();

            LabelNode label = new LabelNode();
            __.add(label);
            __.add(new LineNumberNode(RandomUtils.getRandomInt(), label));
            __.add(new LineNumberNode(RandomUtils.getRandomInt(), label));
            __.add(new LineNumberNode(RandomUtils.getRandomInt(), label));
            __.add(new LineNumberNode(RandomUtils.getRandomInt(), label));
            __.add(new LdcInsnNode(magic));
            __.add(new InsnNode(Opcodes.POP));

            method.instructions.insert(__);
        }
    }

    private MethodNode findOrCreateMethodWrapper(ClassWrapper cw) {
        List<MethodWrapper> filtered = cw.getMethods().stream().filter(mw -> !mw.getAccess().isNative() && !mw.getAccess().isAbstract()).collect(Collectors.toList());
        final MethodNode selectedMethod;
        if (filtered.isEmpty()) {
            selectedMethod = cw.getOrCreateClinit();
        } else {
            selectedMethod = filtered.get(RandomUtils.getRandomInt(filtered.size())).getMethodNode();
        }
        return selectedMethod;
    }

    private String buildWatermarking() {
        StringBuilder builder = new StringBuilder();

        builder.append("build-time: ").append(date);

        return builder.toString();
    }

    private byte[] createMagicInformation(byte[] publicKey, byte[] modules) throws Exception {
        BigInteger exp = new BigInteger(1, publicKey);
        BigInteger mod = new BigInteger(1, modules);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(output);
        byte[] src = buildWatermarking().getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        byte[] nonce = new byte[8];
        SecureRandom random = new SecureRandom();
        random.nextBytes(key);
        random.nextBytes(nonce);
        BigInteger iKey = new BigInteger(1, key);
        BigInteger iNonce = new BigInteger(1, nonce);
        byte[] encoded_key = iKey.modPow(exp, mod).toByteArray();
        byte[] encoded_nonce = iNonce.modPow(exp, mod).toByteArray();
        ChaCha20 crypto = new ChaCha20(key, nonce, 0);
        byte[] encoded_data = new byte[src.length];
        crypto.encrypt(encoded_data, src, src.length);
        stream.writeInt(encoded_key.length);
        stream.write(encoded_key, 0, encoded_key.length);
        stream.writeInt(encoded_nonce.length);
        stream.write(encoded_nonce, 0, encoded_nonce.length);
        stream.writeInt(encoded_data.length);
        stream.write(encoded_data, 0, encoded_data.length);
        return output.toByteArray();
    }
}
