package tech.skidonion.obfuscator.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public class VerifyUtils {
    public static JsonObject requestSoftwareInformation(String url, String uid, String token, String softwareId) {
        Map<String, String> headers = genericHeader(uid, token);
        Map<String, String> params = new HashMap<>();
        params.put("software_id", softwareId);
        return JsonParser.parseString(HttpUtils.post(url + "api/admin/software-information", params, headers)).getAsJsonObject();
    }

    private static Map<String, String> genericHeader(String uid, String token) {
        return new HashMap<String, String>() {
            {
                put("phantom-shield-x-uid", uid);
                put("phantom-shield-x-api-token", token);
            }
        };
    }

}
