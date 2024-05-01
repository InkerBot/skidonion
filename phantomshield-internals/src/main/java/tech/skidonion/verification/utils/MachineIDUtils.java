package tech.skidonion.verification.utils;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;
import tech.skidonion.obfuscator.inline.Inline;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MachineIDUtils {

    /**
     * @param array 数组最后一个参数为返回值
     */
    @NativeObfuscation.Inline
    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE, manualTryCatch = true)
    public static void generate(Object[] array) {
        Objects.requireNonNull(array);
        byte[] UNIQUE = new byte[]{82, (byte) 249, (byte) 163, (byte) 203, (byte) 143, 107, (byte) 129, 8};
        int max = 255 / 2;
        int current = 0;
        current += 6; // for head and tail
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bos);
        int length = ThreadLocalRandom.current().nextInt(4, 9);
        current += length;
        try {
            data.writeByte(0xFF);
            data.writeByte(0x02);
            for (int i = 0; i < length; i++) data.write((byte) ThreadLocalRandom.current().nextInt(1, 256));
            data.write(0x00);
            host:
            {
                String host = InetAddress.getLocalHost().getHostName();
                Inline.trycatch();
                byte[] n = host.getBytes(StandardCharsets.UTF_8);
                current += 3 + n.length;
                if (current > max) break host;
                data.writeByte(0x01);
                data.writeShort(n.length & 0xFFFF);
                data.write(n, 0, n.length);
            }
            os:
            {
                String os = String.join("-", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"), System.getProperty("user.name"));
                byte[] n = os.getBytes(StandardCharsets.UTF_8);
                current += 3 + n.length;
                if (current > max) break os;
                data.writeByte(0x02);
                data.writeShort(n.length & 0xFFFF);
                data.write(n, 0, n.length);
            }

            uuid:
            {
                current++;
                data.writeByte(0x03);
                Path path = Paths.get(System.getProperty("user.home"), ".skidonion");
                byte[] uuid;
                if (!Files.exists(path)) {
                    uuid = new byte[16];
                    ThreadLocalRandom.current().nextBytes(uuid);
                    Files.write(path, uuid);
                } else {
                    uuid = Files.readAllBytes(path);
                }
                Inline.trycatch();
                current += 1 + uuid.length;
                if (current > max) break uuid;
                data.write(uuid.length & 0xFF);
                data.write(uuid, 0, uuid.length);
            }
            data.writeByte(0x00);
            int rest = max - current;
            for (int i = 0; i < rest; i++) data.write((byte) ThreadLocalRandom.current().nextInt(1, 256));
            data.writeByte(0x02);
            data.writeByte(0xFF);
        } catch (IOException exception) {
            return;
        }


        StringBuilder sb = new StringBuilder();
        byte[] byteArray = bos.toByteArray();
        for (int i = 0; i < byteArray.length; i++) {
            byte b = byteArray[i];
            sb.append(Integer.toHexString(((b ^ UNIQUE[i % UNIQUE.length]) & 0xFF) | 0x100), 1, 3);
        }
        array[array.length - 1] = sb.toString();
    }

    /**
     * ((long) array[0] >> 32 ^ rand) & 0b1) == 1
     *
     * @param array 数组大小至少为3，index = 0时，为返回值，index = size - 1时为要判断的hwid index = size - 2时为输入的随机数字
     */
    @NativeObfuscation.Inline
    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE, manualTryCatch = true)
    public static void check(Object[] array) {
        Objects.requireNonNull(array);
        byte[] UNIQUE = new byte[]{82, (byte) 249, (byte) 163, (byte) 203, (byte) 143, 107, (byte) 129, 8};

        String hexString = (String) array[array.length - 1];
        int l = hexString.length();
        byte[] encoded = new byte[l / 2];
        for (int i = 0; i < l; i += 2) {
            encoded[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                     + Character.digit(hexString.charAt(i + 1), 16));
        }

        byte[] decoded = new byte[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            decoded[i] = (byte) (encoded[i] ^ UNIQUE[i % UNIQUE.length]);
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
        DataInputStream data = new DataInputStream(bis);
        try {
            int valid = 0;
            byte[] mark = new byte[]{(byte) 0xFF, 0x02};
            byte[] header = new byte[2];
            byte[] tail = new byte[]{decoded[decoded.length - 1], decoded[decoded.length - 2]};
            data.read(header);
            if (!Arrays.equals(header, mark) || !Arrays.equals(tail, mark)) {
                array[0] = 0x1010_1010_1010_1010L;
                return;
            }
            byte n;
            do {
                n = data.readByte();
            } while (n != 0x00);
            block:
            do {
                n = data.readByte();
                switch (n) {
                    case 0x1: {
                        short length = data.readShort();
                        byte[] bytes = new byte[length];
                        data.read(bytes, 0, bytes.length);
                        String host = InetAddress.getLocalHost().getHostName();
                        Inline.trycatch();
                        if (Arrays.equals(host.getBytes(StandardCharsets.UTF_8), bytes)) {
                            valid++;
                        }
                        break;
                    }
                    case 0x2: {
                        short length = data.readShort();
                        byte[] bytes = new byte[length];
                        data.read(bytes, 0, bytes.length);
                        if (Arrays.equals(String.join("-", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"), System.getProperty("user.name")).getBytes(StandardCharsets.UTF_8), bytes))
                            valid++;
                        break;
                    }
                    case 0x3: {
                        byte length = data.readByte();
                        byte[] bytes = new byte[length];
                        data.read(bytes, 0, bytes.length);
                        Path path = Paths.get(System.getProperty("user.home"), ".skidonion");
                        if (Files.exists(path) && Arrays.equals(Files.readAllBytes(path), bytes)) {
                            valid++;
                        }
                        break block;
                    }
                }
            } while (n != 0x00);

            if (valid >= 3) {
                long rand = Math.abs(ThreadLocalRandom.current().nextInt());
                int src = (int) array[array.length - 2];
                rand += 0xFFFF_FFFF_0000_0000L & (((long) (src ^ 0b01)) << 32);
                array[0] = rand;
            }
        } catch (Exception ignore) {
            array[0] = ThreadLocalRandom.current().nextInt();
        }
    }

}
