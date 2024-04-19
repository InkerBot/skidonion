package tech.skidonion.verification.utils;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;
import tech.skidonion.obfuscator.inline.Inline;
import tech.skidonion.verification.crypto.*;
import tech.skidonion.verification.json.Json;
import tech.skidonion.verification.json.JsonArray;
import tech.skidonion.verification.json.JsonObject;
import tech.skidonion.verification.json.JsonValue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class VerifyUtils {
    private static final Random RANDOM = new SecureRandom();
    private final static Map<Integer, byte[]> CLOUD_CONSTANT_MAP = new HashMap<>();
    private final static Map<String, LocalDateTime> EXPIRED_DATE = new HashMap<>();

    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE, manualTryCatch = true)
    public static int login(String username, String password) {
        int r = RANDOM.nextInt();
        byte result = -1;
        Map<String, String> headers = new HashMap<>();
        if (Internals.getVerifyToken() != null) headers.put("verify-token", Internals.getVerifyToken());
        Map<String, String> params = new HashMap<>();
        try {
            params.put("username", URLEncoder.encode(username));
            params.put("password", URLEncoder.encode(password));
            params.put("software_id", String.valueOf(Internals.softwareId()));

            BigInteger privateKey = new BigInteger(1536, RANDOM);
            BigInteger m = new BigInteger(2048, RANDOM);
            BigInteger q = new BigInteger(1024, RANDOM);
            BigInteger p = q.modPow(privateKey, m);
            params.put("p", URLEncoder.encode(Base64.encode(p.toByteArray())));
            params.put("q", URLEncoder.encode(Base64.encode(q.toByteArray())));
            params.put("m", URLEncoder.encode(Base64.encode(m.toByteArray())));
            String res = HttpUtils.post(Internals.verificationServer() + "api/verify/login", params, headers);
            if (res != null) {
                JsonObject json = Json.parse(res).asObject();
                result = (byte) json.getInt("code", -1);
                if (result == 0) {
                    JsonObject entity = json.get("entity").asObject();
                    JsonObject data = entity.get("data").asObject();
                    String signature = entity.getString("signature", "");
                    Internals.setUserId(data.getLong("uid", -1));
                    Internals.setUsername(username);
                    Internals.setVerifyToken(data.getString("jwt", ""));

                    EdDSAEngine verify = new EdDSAEngine();
                    verify.initVerify(new EdDSAPublicKey(Base64.decode(Internals.publicKey())));
                    if (!verify.verify(data.toString().getBytes(StandardCharsets.UTF_8), Base64.decode(signature))) {
                        result = -2;
                        return r & 0xFFFF00FF | (result & 0xFF) << 8;
                    }
                    Internals.setNonce(Base64.decode(data.getString("n", "==")));
                    BigInteger s = new BigInteger(1, Base64.decode(data.getString("p", "=="))).modPow(privateKey, m);
                    byte[] src = s.toByteArray();
                    byte[] key = new byte[32];
                    System.arraycopy(src, src.length - 32, key, 0, 32);
                    Internals.setKey(key);
                    Internals.setCrypto(new ChaCha20(Internals.getKey(), Internals.getNonce(), 0));

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
                        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(VerifyUtils::daemonFactory);
                        service.scheduleAtFixedRate(VerifyUtils::heartbeat, 4, 4, TimeUnit.MINUTES);
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


    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE)
    private static Optional<Byte> requestInformation() {
        long delay = 0;
        Map<String, String> headers = new HashMap<>();
        if (Internals.getVerifyToken() != null) headers.put("verify-token", Internals.getVerifyToken());
        Map<String, String> params = new HashMap<>();
        JsonObject p = Json.object();
        p.add("t", System.currentTimeMillis());
        p.add("+", Json.NULL);
        JsonArray q = Json.array(QQUtils.getAllQQ().toArray(new String[0]));
        p.add("q", q);
        p.add("v", Internals.version());
        String[] hwid = new String[1];
//        MachineIDUtils.generate(hwid);
        Inline._verification_generateHardwareID(hwid);
        p.add("h", hwid[0]);

        byte[] src = p.toString().getBytes(StandardCharsets.UTF_8);
        byte[] dst = new byte[src.length];
        ((ChaCha20) Internals.getCrypto()).encrypt(dst, src, src.length);

        params.put("data", URLEncoder.encode(Base64.encode(dst)));
        try {
            String res = HttpUtils.post(Internals.verificationServer() + "api/verify/heartbeat", params, headers);
            long lastTimestamp = System.currentTimeMillis();
            if (res != null) {
                JsonObject json = Json.parse(res).asObject();
                byte code = (byte) json.getInt("code", -1);
                if (code == 0) {
                    JsonObject entity = json.get("entity").asObject();
                    String data = entity.getString("data", "==");
                    String signature = entity.getString("signature", "");

                    src = Base64.decode(data);
                    dst = new byte[src.length];
                    ((ChaCha20) Internals.getCrypto()).decrypt(dst, src, src.length);
                    JsonObject result = Json.parse(new String(dst, StandardCharsets.UTF_8)).asObject();

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
                    array[4] = hwid[0];
//                    MachineIDUtils.check(array);
                    Inline._verification_checkHardwareID(array);
                    if ((((long) array[0] >> 32 ^ rand) & 0b1) != 1) {
                        return Optional.of((byte) -2);
                    }

                    EdDSAEngine verify = new EdDSAEngine();
                    verify.initVerify(new EdDSAPublicKey(Base64.decode(Internals.publicKey())));
                    if (!verify.verify(result.toString().getBytes(StandardCharsets.UTF_8), Base64.decode(signature))) {
                        return Optional.of((byte) -3);
                    }
                    Internals.setMagicKey(Base64.decode(result.getString("m", "==")));
                    for (JsonValue c : result.get("c").asArray()) {
                        JsonObject mem = (JsonObject) c;
                        CLOUD_CONSTANT_MAP.put(Integer.parseInt(mem.getString("h", "-1")), Base64.decode(mem.getString("e", "==")));
                    }
                }
                return Optional.of(code);
            }
        } catch (Exception ignore) {
        }
        return Optional.empty();
    }

    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE)
    private static void heartbeat() {
        try {
            Map<String, String> headers = new HashMap<>();
            if (Internals.getVerifyToken() != null) headers.put("verify-token", Internals.getVerifyToken());
            Map<String, String> params = new HashMap<>();
            JsonObject p = Json.object();
            p.add("t", System.currentTimeMillis());
            p.add("_", Json.NULL);

            byte[] src = p.toString().getBytes(StandardCharsets.UTF_8);
            byte[] dst = new byte[src.length];
            ((ChaCha20) Internals.getCrypto()).encrypt(dst, src, src.length);

            params.put("data", URLEncoder.encode(Base64.encode(dst)));

            String res = HttpUtils.post(Internals.verificationServer() + "api/verify/heartbeat", params, headers);
            if (res != null) {
                JsonObject json = Json.parse(res).asObject();
                byte code = (byte) json.getInt("code", -1);
                if (code == 0) {
                    JsonObject entity = json.get("entity").asObject();
                    String data = entity.getString("data", "==");
                    src = Base64.decode(data);
                    dst = new byte[src.length];
                    ((ChaCha20) Internals.getCrypto()).decrypt(dst, src, src.length);
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

    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE)
    public static void setAsSuspected(String reason) {
        Map<String, String> headers = new HashMap<>();
        if (Internals.getVerifyToken() != null) headers.put("verify-token", Internals.getVerifyToken());

        Map<String, String> params = new HashMap<>();
        JsonObject p = Json.object();
        p.add("t", System.currentTimeMillis());
        p.add("-", Json.NULL);
        p.add("r", reason == null ? "主动风控" : reason);

        byte[] src = p.toString().getBytes(StandardCharsets.UTF_8);
        byte[] dst = new byte[src.length];
        ((ChaCha20) Internals.getCrypto()).encrypt(dst, src, src.length);

        params.put("data", URLEncoder.encode(Base64.encode(dst)));
        try {
            HttpUtils.post(Internals.verificationServer() + "api/verify/heartbeat", params, headers);
        } catch (Exception ignore) {
        }
        System.exit(0);
    }

    /**
     * "xxxx用户组".hashcode();
     */
    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE)
    public static Optional<String> getCloudConstant(int hash, int index) {
        byte[] encoded = CLOUD_CONSTANT_MAP.get(hash);
        if (encoded == null) {
            return Optional.empty();
        }
        int magicKey = 0x0;
        byte[] magic = Internals.getMagicKey();
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
        ChaCha20 crypto = new ChaCha20(Internals.getKey(), Internals.getNonce(), magicKey);
        byte[] dst = new byte[encoded.length];
        crypto.decrypt(dst, encoded, encoded.length);
        int i = 0;
        int point = 0;
        while (point < dst.length) {
            short length = (short) ((dst[point++] & 0xFF) + ((dst[point++] & 0xFF) << 8));
            if (index == i++) {
                return Optional.of(new String(dst, point, length, StandardCharsets.UTF_8));
            }
            point += length;
        }
        return Optional.empty();
    }

    public static String getVerifyToken() {
        return Internals.getVerifyToken();
    }

    public static Optional<LocalDateTime> getExpiredDate(String role) {
        return Optional.ofNullable(EXPIRED_DATE.get(role));
    }

    public static Map<String, LocalDateTime> getExpiredDates() {
        return EXPIRED_DATE;
    }

    public static boolean hasRole(String role) {
        return EXPIRED_DATE.get(role) != null;
    }

    public static Optional<Long> getUserId() {
        if (Internals.getUserId() > 0) {
            return Optional.of(Internals.getUserId());
        }
        return Optional.empty();
    }

    public static Optional<String> getUsername() {
        if (Internals.getUsername() != null) {
            return Optional.of(Internals.getUsername());
        }
        return Optional.empty();
    }


}
