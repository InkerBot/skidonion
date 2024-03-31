package tech.skidonion.obfuscator.downloader;

import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.utils.timer.MSTimer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.Future;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;
import static tech.skidonion.obfuscator.PhantomShield.TRANSLATION;

public class DownloadBuilder {
    private String url;
    private File output;
    private int size;
    private DownloadCallback callback;
    private Runnable onSuccess;
    private Runnable onFailure;
    private MSTimer timer = new MSTimer();

    // callback method per second
    private int cps = 1;

    public DownloadBuilder setUrl(String url) {
        this.url = url;
        return this;
    }

    public DownloadBuilder setOutput(File output) {
        this.output = output;
        return this;
    }

    public Future<DownloadResult> start() {
        Objects.requireNonNull(url, "url is null");
        Objects.requireNonNull(output, "output is null");
        return PhantomShield.EXECUTOR.submit(this::download);
    }

    private DownloadResult download() {
        try {
            HttpURLConnection conn = createHttpURLConnection();
            try (InputStream is = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(output);) {
                size = conn.getContentLength();
                int length;
                byte[] n = new byte[4096];
                timer.reset();
                while ((length = is.read(n)) != -1) {
                    fos.write(n, 0, length);
                    if (timer.reached(1000 / cps)) {
                        timer.reset();
                        if (callback != null) {
                            callback.accept((float) fos.getChannel().size() / size);
                        }
                    }
                }
                conn.disconnect();
            }
        } catch (Exception e) {
            ERROR(TRANSLATION("phantom-shield-x.download.failed"), e);
            if (onFailure != null) {
                onFailure.run();
            }
        }

        if (onSuccess != null) {
            onSuccess.run();
        }
        return new DownloadResult(url, output, size);
    }

    private HttpURLConnection createHttpURLConnection() throws IOException {
        URL httpUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) httpUrl.openConnection();
        conn.setReadTimeout(15000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        if (conn.getResponseCode() >= 300) {
            throw new RuntimeException("HTTP Request is not success, Response code is " + conn.getResponseCode());
        }
        return conn;
    }


    public DownloadBuilder setCallback(DownloadCallback callback) {
        this.callback = callback;
        return this;
    }

    public DownloadBuilder setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public DownloadBuilder setOnFailure(Runnable onFailure) {
        this.onFailure = onFailure;
        return this;
    }

    @FunctionalInterface
    public interface DownloadCallback {
        // 0.0f ~ 1.0f
        void accept(float progress);
    }
}
