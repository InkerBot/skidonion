package tech.skidonion.verification.utils;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;
import tech.skidonion.obfuscator.inline.Inline;
import tech.skidonion.verification.crypto.*;
import tech.skidonion.verification.crypto.Base64;
import tech.skidonion.verification.json.Json;
import tech.skidonion.verification.json.JsonArray;
import tech.skidonion.verification.json.JsonObject;
import tech.skidonion.verification.json.JsonValue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class VerifyUtils {
    @NativeObfuscation.Inline
    private static Random RANDOM;
    @NativeObfuscation.Inline
    private static Map<Integer, byte[]> CLOUD_CONSTANT_MAP;
    @NativeObfuscation.Inline
    private static Map<String, LocalDateTime> EXPIRED_DATE;
    @NativeObfuscation.Inline
    private static byte[] NONCE;
    @NativeObfuscation.Inline
    private static ChaCha20 CRYPTO;
    @NativeObfuscation.Inline
    private static String VERIFY_TOKEN;
    @NativeObfuscation.Inline
    private static byte[] KEY;
    @NativeObfuscation.Inline
    private static String USERNAME;
    @NativeObfuscation.Inline
    private static long USER_ID;
    @NativeObfuscation.Inline
    private static byte[] MAGIC_KEY;
    @NativeObfuscation.Inline
    private static String NICKNAME;

    @NativeObfuscation.Inline
    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE, manualTryCatch = true)
    public static int login(String username, String password, boolean useHashedPassword) {
        if (RANDOM == null) {
            RANDOM = new SecureRandom();
            CLOUD_CONSTANT_MAP = new HashMap<>();
            EXPIRED_DATE = new HashMap<>();
        }
        Inline.processEnvironment();
        int r = RANDOM.nextInt();
        byte result = -1;
        Map<String, String> headers = new HashMap<>();
        if (VERIFY_TOKEN != null) headers.put("verify-token", VERIFY_TOKEN);
        Map<String, String> params = new HashMap<>();
        try {
            params.put("username", URLEncoder.encode(username));
            params.put("password", URLEncoder.encode(password));
            params.put("software_id", String.valueOf(Internals.softwareId()));


            ByteBuffer keypairBuffer = ByteBuffer.allocateDirect(32 + 64);
            byte[] publicKey = new byte[32];
            RANDOM.nextBytes(publicKey);
            keypairBuffer.put(publicKey);
            Internals.ed25519generate(keypairBuffer);
            byte[] privateKey = new byte[64];
            keypairBuffer.position(0);
            keypairBuffer.get(publicKey);
            keypairBuffer.get(privateKey);
            params.put("s", URLEncoder.encode(Base64.encode(publicKey)));

            params.put("e", URLEncoder.encode(String.valueOf(useHashedPassword)));
            String res = HttpUtils.post(Internals.verificationServer() + "api/v2/verify/login", params, headers);
            Inline.trycatch();
            if (res != null) {
                JsonObject json = Json.parse(res).asObject();
                result = (byte) json.getInt("code", -1);
                if (result == 0) {
                    JsonObject entity = json.get("entity").asObject();
                    JsonObject data = entity.get("data").asObject();
                    String signature = entity.getString("signature", "");
                    USER_ID = (data.getLong("uid", -1));
                    USERNAME = (username);
                    VERIFY_TOKEN = (data.getString("jwt", ""));
                    NICKNAME = (data.getString("nickname", ""));


                    byte[] message = data.toString().getBytes(StandardCharsets.UTF_8);
                    int messageLen = message.length;
                    ByteBuffer verifyBuffer = ByteBuffer.allocateDirect(32 * 4 + 64 + 4 + messageLen);
                    verifyBuffer.put(Internals.publicKey());
                    verifyBuffer.put(Base64.decode(signature));
                    verifyBuffer.put((byte) (messageLen & 0xff));
                    verifyBuffer.put((byte) (messageLen >> 8 & 0xff));
                    verifyBuffer.put((byte) (messageLen >> 16 & 0xff));
                    verifyBuffer.put((byte) (messageLen >> 24 & 0xff));
                    verifyBuffer.put(message);

                    if (!Internals.ed25519verify(verifyBuffer)) {
                        result = -2;
                        return r & 0xFFFF00FF | (result & 0xFF) << 8;
                    }

                    byte[] raw = Base64.decode(data.getString("n", "=="));
                    byte[] decoded = new byte[12];
                    if (raw.length == 48) {
                        int[] encoded = new int[12];
                        for (int i = 0; i < 12; i++)
                            encoded[i] = (raw[i * 4] & 0xff) | (raw[i * 4 + 1] & 0xff) << 8 | (raw[i * 4 + 2] & 0xff) << 16 | (raw[i * 4 + 3] & 0xff) << 24;
                        Internals.polyXor(encoded, decoded);
                    }

                    NONCE = (decoded);

                    ByteBuffer exchangeBuffer = ByteBuffer.allocateDirect(32 * 4 + 64);
                    exchangeBuffer.put(Internals.publicKey());
                    exchangeBuffer.put(privateKey);
                    Internals.ed25519exchange(exchangeBuffer);

                    byte[] key = new byte[32];
                    exchangeBuffer.position(32 + 12);
                    exchangeBuffer.get(key);
                    KEY = (key);
                    CRYPTO = (new ChaCha20(KEY, NONCE, 0));

                    JsonArray roles = data.get("roles").asArray();
                    for (int i = 0; i < roles.size(); i++) {
                        JsonObject role = roles.get(i).asObject();
                        EXPIRED_DATE.put(role.getString("rank_name", String.valueOf(i)), LocalDateTime.parse(role.getString("expired_date", "1970-1-1T00:00:00")));
                    }

                    Optional<Byte> requestResult = requestInformation();
                    if (!requestResult.isPresent()) {
                        result = -3;
                        return r & 0xFFFF00FF | (result & 0xFF) << 8;
                    }
                    result = requestResult.get();
                    if (result != 0) {
                        result += 100;
                    } else {
                        if (Internals.shouldKeepAlive()) {
                            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(VerifyUtils::daemonFactory);
                            service.scheduleAtFixedRate(VerifyUtils::heartbeat, 4, 4, TimeUnit.MINUTES);
                        }
                    }
                }
            }
        } catch (Exception e) {
            result = -1;
        }
        return r & 0xFFFF00FF | (result & 0xFF) << 8;
    }

    private static Thread daemonFactory(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    }


    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE, manualTryCatch = true)
    @NativeObfuscation.Inline
    private static Optional<Byte> requestInformation() {
        Map<String, String> headers = new HashMap<>();
        if (VERIFY_TOKEN != null) headers.put("verify-token", VERIFY_TOKEN);
        Map<String, String> params = new HashMap<>();
        JsonObject p = Json.object();
        p.add("t", System.currentTimeMillis());
        p.add("+", Json.NULL);
        JsonArray q = Json.array(QQUtils.getAllQQ().toArray(new String[0]));
        p.add("q", q);
        p.add("v", Internals.version());
        String[] hwid = new String[1];
        MachineIDUtils.generate(hwid);
        p.add("h", hwid[0]);

        byte[] src = p.toString().getBytes(StandardCharsets.UTF_8);
        byte[] dst = CRYPTO.xor(src);

        params.put("data", URLEncoder.encode(Base64.encode(dst)));
        try {
            String res = HttpUtils.post(Internals.verificationServer() + "api/v2/verify/heartbeat", params, headers);
            Inline.trycatch();
            long lastTimestamp = System.currentTimeMillis();
            if (res != null) {
                JsonObject json = Json.parse(res).asObject();
                byte code = (byte) json.getInt("code", -1);
                if (code == 0) {
                    JsonObject entity = json.get("entity").asObject();
                    String data = entity.getString("data", "==");
                    String signature = entity.getString("signature", "");

                    src = Base64.decode(data);
                    dst = CRYPTO.xor(src);
                    JsonObject result = Json.parse(new String(dst, StandardCharsets.UTF_8)).asObject();

                    long delay = System.currentTimeMillis() - lastTimestamp;
                    long now = System.currentTimeMillis();
                    long timestamp = result.getLong("t", -1);
                    long diff = now - timestamp - delay;
                    if (diff < 0) diff = -diff;
                    if (diff > 60000L) {
                        return Optional.of((byte) -1);
                    }
                    int rand = RANDOM.nextInt();
                    Object[] array = new Object[5];
                    array[0] = RANDOM.nextInt();
                    array[1] = RANDOM.nextInt();
                    array[2] = RANDOM.nextInt();
                    array[3] = rand;
                    array[4] = result.getString("h", "==");
                    MachineIDUtils.check(array);
                    if (((((Number) array[0]).longValue() >> 32 ^ rand) & 0b1) != 1) {
                        return Optional.of((byte) -2);
                    }

                    byte[] message = result.toString().getBytes(StandardCharsets.UTF_8);
                    int messageLen = message.length;
                    ByteBuffer verifyBuffer = ByteBuffer.allocateDirect(32 * 4 + 64 + 4 + messageLen);
                    verifyBuffer.put(Internals.publicKey());
                    verifyBuffer.put(Base64.decode(signature));
                    verifyBuffer.put((byte) (messageLen & 0xff));
                    verifyBuffer.put((byte) (messageLen >> 8 & 0xff));
                    verifyBuffer.put((byte) (messageLen >> 16 & 0xff));
                    verifyBuffer.put((byte) (messageLen >> 24 & 0xff));
                    verifyBuffer.put(message);

                    if (!Internals.ed25519verify(verifyBuffer)) {
                        return Optional.of((byte) -3);
                    }
                    MAGIC_KEY = (Base64.decode(result.getString("m", "==")));
                    for (JsonValue c : result.get("c").asArray()) {
                        JsonObject mem = (JsonObject) c;
                        CLOUD_CONSTANT_MAP.put(Integer.parseInt(mem.getString("h", "-1")), Base64.decode(mem.getString("e", "==")));
                    }
                    Internals.initBuffer();
                    for (JsonValue k : result.get("k").asArray()) {
                        JsonObject mem = (JsonObject) k;
                        int hash = Integer.parseInt(mem.getString("h", "-1"));
                        byte[] des = new byte[32];
                        int magicKey = 0x0;
                        byte[] magic = MAGIC_KEY;
                        int base = 0x0;
                        for (int i = 0; i < 16; i++) {
                            base = base | magic[i] & 0xFF;
                            if (i % 4 == 3) {
                                magicKey ^= base;
                                base = 0x0;
                            } else {
                                base <<= 8;
                            }
                        }
                        ChaCha20 crypto = new ChaCha20(KEY, NONCE, (long) magicKey * hash * 37);
                        byte[] src_key = Base64.decode(mem.getString("e", "=="));
                        byte[] magic2 = crypto.xor(src_key);
                        byte[] session = Internals.sessionKey();

                        for (int i = des.length - 1; i >= 0; i--) {
                            int index = i / 2;
                            int position = index % 2;
                            if (i % 2 == 0) {
                                des[i] = magic2[index + (position == 1 ? -1 : 1)];
                            } else {
                                des[i] = session[index + (position == 1 ? -1 : 1)];
                            }
                        }
                        byte temp = des[0];
                        des[0] = des[des.length - 1];
                        des[des.length - 1] = temp;

                        Internals.decryptClasses(hash, des);
                    }
                }
                return Optional.of(code);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return Optional.empty();
    }

    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE, manualTryCatch = true)
    private static void heartbeat() {
        try {
            Map<String, String> headers = new HashMap<>();
            if (VERIFY_TOKEN != null) headers.put("verify-token", VERIFY_TOKEN);
            Map<String, String> params = new HashMap<>();
            JsonObject p = Json.object();
            p.add("t", System.currentTimeMillis());
            p.add("_", Json.NULL);

            byte[] src = p.toString().getBytes(StandardCharsets.UTF_8);
            byte[] dst = CRYPTO.xor(src);

            params.put("data", URLEncoder.encode(Base64.encode(dst)));

            String res = HttpUtils.post(Internals.verificationServer() + "api/v2/verify/heartbeat", params, headers);
            Inline.trycatch();
            if (res != null) {
                JsonObject json = Json.parse(res).asObject();
                byte code = (byte) json.getInt("code", -1);
                if (code == 0) {
                    JsonObject entity = json.get("entity").asObject();
                    String data = entity.getString("data", "==");
                    src = Base64.decode(data);
                    dst = CRYPTO.xor(src);
                    JsonObject result = Json.parse(new String(dst, StandardCharsets.UTF_8)).asObject();
                    if (result.get("b") != null) {
                        System.exit(0);
                    }
                } else {
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            System.exit(0);
        }
    }

    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE, manualTryCatch = true)
    @NativeObfuscation.Inline
    public static void setAsSuspected(String reason) {
        Map<String, String> headers = new HashMap<>();
        if (VERIFY_TOKEN != null) headers.put("verify-token", VERIFY_TOKEN);

        Map<String, String> params = new HashMap<>();
        JsonObject p = Json.object();
        p.add("t", System.currentTimeMillis());
        p.add("-", Json.NULL);
        p.add("r", reason == null ? "主动风控" : reason);

        byte[] src = p.toString().getBytes(StandardCharsets.UTF_8);
        byte[] dst = CRYPTO.xor(src);

        params.put("data", URLEncoder.encode(Base64.encode(dst)));
        try {
            HttpUtils.post(Internals.verificationServer() + "api/v2/verify/heartbeat", params, headers);
            Inline.trycatch();
        } catch (Exception ignore) {
        }
        System.exit(0);
    }

    /**
     * "xxxx用户组".hashcode();
     */
    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE, manualTryCatch = true)
    @NativeObfuscation.Inline
    public static Optional<String> getCloudConstant(int hash, int index) {
        byte[] encoded = CLOUD_CONSTANT_MAP.get(hash);
        if (encoded == null) {
            return Optional.empty();
        }
        int magicKey = 0x0;
        byte[] magic = MAGIC_KEY;
        int base = 0x0;
        for (int i = 0; i < 16; i++) {
            base = base | magic[i] & 0xFF;
            if (i % 4 == 3) {
                magicKey ^= base;
                base = 0x0;
            } else {
                base <<= 8;
            }
        }

        ChaCha20 crypto = new ChaCha20(KEY, NONCE, (long) magicKey * hash * 13);
//        byte[] dst = crypto.xor(encoded);
        int i = 0;
        int point = 0;
        while (point < encoded.length) {
            short length = (short) ((crypto.xor(new byte[]{encoded[point++]})[0] & 0xFF) + ((crypto.xor(new byte[]{encoded[point++]})[0] & 0xFF) << 8));
            if (index == i++) {
                byte[] dst = new byte[length];
                System.arraycopy(encoded, point, dst, 0, length);
                return Optional.of(new String(crypto.xor(dst), 0, length, StandardCharsets.UTF_8));
            } else {
                crypto.skip(length);
            }
            point += length;
        }
        return Optional.empty();
    }

    @NativeObfuscation.Inline
    public static String getVerifyToken() {
        return VERIFY_TOKEN;
    }

    @NativeObfuscation.Inline
    public static Optional<LocalDateTime> getExpiredDate(String role) {
        return Optional.ofNullable(EXPIRED_DATE.get(role));
    }

    @NativeObfuscation.Inline
    public static Map<String, LocalDateTime> getExpiredDates() {
        return EXPIRED_DATE;
    }

    @NativeObfuscation.Inline
    public static boolean hasRole(String role) {
        return EXPIRED_DATE.get(role) != null;
    }

    @NativeObfuscation.Inline
    public static Optional<Long> getUserId() {
        if (USER_ID > 0) {
            return Optional.of(USER_ID);
        }
        return Optional.empty();
    }

    @NativeObfuscation.Inline
    public static Optional<String> getUsername() {
        if (USERNAME != null) {
            return Optional.of(USERNAME);
        }
        return Optional.empty();
    }

    @NativeObfuscation.Inline
    public static Optional<String> getNickname() {
        if (NICKNAME != null) {
            return Optional.of(USERNAME);
        }
        return Optional.empty();
    }


}
