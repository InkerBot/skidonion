package tech.skidonion.obfuscator.downloader;

import java.io.File;

public class DownloadResult {
    private final String url;
    private final int size;
    private final File output;

    public DownloadResult(String url, File output, int size) {
        this.url = url;
        this.output = output;
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public int getSize() {
        return size;
    }

    public File getOutput() {
        return output;
    }

}
