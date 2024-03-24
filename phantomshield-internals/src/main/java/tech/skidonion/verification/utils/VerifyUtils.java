package tech.skidonion.verification.utils;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;
import tech.skidonion.verification.crypto.Base64;
import tech.skidonion.verification.crypto.ChaCha20;
import tech.skidonion.verification.crypto.EdDSAEngine;
import tech.skidonion.verification.crypto.EdDSAPublicKey;
import tech.skidonion.verification.json.Json;
import tech.skidonion.verification.json.JsonArray;
import tech.skidonion.verification.json.JsonObject;
import tech.skidonion.verification.time.Packet;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class VerifyUtils {
    private static final Random RANDOM = new SecureRandom();
    private static byte[] NONCE;
    private static ChaCha20 CRYPTO;
    private static String VERIFY_TOKEN;
    private static byte[] KEY;
    private final static Map<Integer, List<byte[]>> CLOUD_CONSTANT_MAP = new HashMap<>();
    private static String USERNAME;
    private static long USER_ID;
    private static String MAGIC_KEY;
    private final static Map<String, LocalDateTime> EXPIRED_DATE = new HashMap<>();

    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_RED)
    public static int login(String username, String password) {
        int r = RANDOM.nextInt();
        long delay = 0;
        byte result = -1;
        Map<String, String> headers = genericHeader();
        Map<String, String> params = new HashMap<>();
        try {
            params.put("username", URLEncoder.encode(username, "utf8"));
            params.put("password", URLEncoder.encode(password, "utf8"));
            params.put("software_id", String.valueOf(Internals.softwareId()));

            BigInteger privateKey = new BigInteger(1536, RANDOM);
            BigInteger m = new BigInteger(2048, RANDOM);
            BigInteger q = new BigInteger(1024, RANDOM);
            BigInteger p = q.modPow(privateKey, m);
            params.put("p", URLEncoder.encode(Base64.encode(p.toByteArray()), "utf8"));
            params.put("q", URLEncoder.encode(Base64.encode(q.toByteArray()), "utf8"));
            params.put("m", URLEncoder.encode(Base64.encode(m.toByteArray()), "utf8"));
            String res = HttpUtils.post(Internals.verificationServer() + "api/verify/login", params, headers);
            long lastTimestamp = System.currentTimeMillis();
            if (res != null) {
                JsonObject json = Json.parse(res).asObject();
                result = (byte) json.getInt("code", -1);
                if (result == 0) {
                    JsonObject entity = json.get("entity").asObject();
                    JsonObject data = entity.get("data").asObject();
                    String signature = entity.getString("signature", "");
                    USER_ID = data.getLong("uid", -1);
                    if (USER_ID <= 0) {
                        result = -1;
                        throw new RuntimeException();
                    }
                    USERNAME = username;
                    VERIFY_TOKEN = data.getString("jwt", "");

                    DatagramSocket socket = new DatagramSocket();
                    socket.setSoTimeout(2000);
                    Packet ntpPacket = new Packet();
                    long t0 = System.currentTimeMillis();
                    ntpPacket.setTransmitTimestamp(new Packet.Timestamp(t0));
                    byte[] ntpRawPacket = ntpPacket.asByteArray();
                    DatagramPacket packet = new DatagramPacket(ntpRawPacket, ntpRawPacket.length, InetAddress.getByName("time.windows.com"), 123);
                    socket.send(packet);
                    socket.receive(packet);
                    long t3 = System.currentTimeMillis();
                    Packet recvPacket = new Packet(ByteBuffer.wrap(packet.getData()));
                    long t1 = recvPacket.getReceiveTimestamp().getTimeMillis();
                    long t2 = recvPacket.getTransmitTimestamp().getTimeMillis();
                    socket.close();
                    delay += System.currentTimeMillis() - lastTimestamp;
                    long now = System.currentTimeMillis() + (t1 + t2 - t0 - t3 + 1) / 2;
                    long timestamp = data.getLong("t", -1);
                    long diff = now - timestamp - delay;
                    if (diff < 0) diff = -diff;
                    if (diff > 10000L) {
                        result = -1;
                        return r & 0xFFFF00FF | (result & 0xFF) << 8;
                    }
                    EdDSAEngine verify = new EdDSAEngine();
                    verify.initVerify(new EdDSAPublicKey(Base64.decode(Internals.publicKey())));
                    if (!verify.verify(data.toString().getBytes(StandardCharsets.UTF_8), Base64.decode(signature))) {
                        result = -1;
                        return r & 0xFFFF00FF | (result & 0xFF) << 8;
                    }
                    NONCE = Base64.decode(data.getString("n", "=="));
                    BigInteger s = new BigInteger(1, Base64.decode(data.getString("p", "=="))).modPow(privateKey, m);
                    byte[] src = s.toByteArray();
                    byte[] key = new byte[32];
                    System.arraycopy(src, src.length - 32, key, 0, 32);
                    KEY = key;
                    CRYPTO = new ChaCha20(KEY, NONCE, 0);

                    JsonArray roles = data.get("roles").asArray();
                    for (int i = 0; i < roles.size(); i++) {
                        JsonObject role = roles.get(i).asObject();
                        EXPIRED_DATE.put(role.getString("rank_name", String.valueOf(i)), LocalDateTime.parse(role.getString("expired_date", "1970-1-1T00:00:00")));
                    }
                    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
                    service.scheduleWithFixedDelay(VerifyUtils::heartbeat, 4, 4, TimeUnit.MINUTES);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = -1;
        }
        return r & 0xFFFF00FF | (result & 0xFF) << 8;
    }

    public static void heartbeat() {
        Map<String, String> headers = genericHeader();
        Map<String, String> params = new HashMap<>();
        String res = HttpUtils.post(Internals.verificationServer() + "api/verify/heartbeat", params, headers);
        if (res != null) {
            JsonObject json = Json.parse(res).asObject();
            System.out.println(json);

        }
    }

    /*
        "xxxx用户组".hashcode();
     */
    public static Optional<String> getCloudConstant(int hash, int index) {
        return Optional.empty();
    }

    public static Optional<LocalDateTime> getExpiredDate(String role) {
        return Optional.ofNullable(EXPIRED_DATE.get(role));
    }

    public static Map<String, LocalDateTime> getExpiredDates() {
        return EXPIRED_DATE;
    }

    public static boolean hasRole(String role) {
        return false;
    }

    public static void setSuspected(String reason) {

    }

    public static Optional<Long> getUserId() {
        if (USER_ID > 0) {
            return Optional.of(USER_ID);
        }
        return Optional.empty();
    }

    public static Optional<String> getUsername() {
        if (USERNAME != null) {
            return Optional.of(USERNAME);
        }
        return Optional.empty();
    }

    private static Map<String, String> genericHeader() {
        return new HashMap<String, String>() {
            {
                if (VERIFY_TOKEN != null) put("verify-token", VERIFY_TOKEN);
            }
        };
    }


}
