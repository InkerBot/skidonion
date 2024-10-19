package tech.skidonion.obfuscator.utils;

import org.junit.jupiter.api.Test;
import tech.skidonion.obfuscator.inline.Wrapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpUtilsTest {
    private static final String URL = "https://skidonion.tech/api/admin/";

    //    private static final String URL = "http://localhost:8694/api/admin/";
    @Test
    void softwareInformation() {
        Map<String, String> params = new HashMap<>();
        params.put("software_id", "1");
        String result = HttpUtils.post(URL + "software-information", params, header());
        System.out.println(result);
    }

    @Test
    void whois() {
        Map<String, String> params = new HashMap<>();
        params.put("software_id", "1");
        params.put("username", "Paimonqwq");
        String result = HttpUtils.post(URL + "whois", params, header());
        System.out.println(result);
    }

    @Test
    void userInformation() {
        Map<String, String> params = new HashMap<>();
        params.put("software_id", "1");
        params.put("username", "Paimonqwq");
        String result = HttpUtils.post(URL + "user-information", params, header());
        System.out.println(result);
    }

    @Test
    void queryOrder() {
        Map<String, String> params = new HashMap<>();
        params.put("software_id", "1");
        params.put("order_id", "202403312156198059");
        String result = HttpUtils.post(URL + "query-order", params, header());
        System.out.println(result);
    }

    @Test
    void setAsSuspected() {
        Map<String, String> params = new HashMap<>();
        params.put("software_id", "1");
        params.put("username", "imfl0wow");
        params.put("reason", "测试");
        String result = HttpUtils.post(URL + "set-as-suspected", params, header());
        System.out.println(result);
    }

    @Test
    void removeSuspected() {
        Map<String, String> params = new HashMap<>();
        params.put("software_id", "1");
        params.put("username", "imfl0wow");
        String result = HttpUtils.post(URL + "remove-suspected", params, header());
        System.out.println(result);
    }

    @Test
    void generateCard() {
        Map<String, String> params = new HashMap<>();
        params.put("software_id", "1");
        params.put("card_id", "2");
        params.put("amount", "1");
        String result = HttpUtils.post(URL + "generate-card", params, header());
        System.out.println(result);
    }

    @Test
    void userOnline() {
        Map<String, String> params = new HashMap<>();
        params.put("software_id", "1");
        params.put("token", Wrapper.getVerifyToken());
        String result = HttpUtils.post(URL + "user-online", params, header());
        System.out.println(result);
        // {"code":0,"message":"成功","entity":{"data":{"user_id":1,"username":"imfl0wow","software_id":1,"create_time":1712041058922}}}
    }

    @Test
    void setUserExpiredDate() {
        Map<String, String> params = new HashMap<>();
        params.put("software_id", "1");
        params.put("username", "imfl0wow");
        params.put("role_id", "1");
        params.put("expired_date", "2077-01-01 00:00:00");
        String result = HttpUtils.post(URL + "set-user-expired-date", params, header());
        System.out.println(result);
    }

    @Test
    void UsersByQQ() {
        Map<String, String> params = new HashMap<>();
        params.put("qq", "3801318676");
        params.put("software_id", "1");
        String result = HttpUtils.post(URL + "users-by-qq", params, header());
        System.out.println(result);
    }


    public static Map<String, String> header() {
        Map<String, String> header = new HashMap<>();
        header.put("phantom-shield-x-uid", "1"); // 用户ID
        header.put("phantom-shield-x-api-token", "769e4f678db8436b0018fc6fe60a5a7a"); // 用户TOKEN
        return header;
    }

    @Test
    void resolve() {
        try {
            InetAddress addr = InetAddress.getByName("skidonion.tech");
            System.out.println(Arrays.toString(addr.getAddress()));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}