package tech.skidonion.obfuscator.checksum;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AntiTamperWriter {
    private final ZipOutputStream zos;
    private final Map<String, String> checksums = new HashMap<>();

    public AntiTamperWriter(ZipOutputStream zos) {
        this.zos = zos;
    }

    public synchronized void write(ZipEntry entry, byte[] content) throws IOException {
        addEntryChecksum(entry, content);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }

    public void close(String comment) throws IOException {
        writeStub();
        zos.setComment(comment);
        zos.close();
    }

    private void addEntryChecksum(ZipEntry entry, byte[] content) {
        int checksum = 0;
        for (byte b : content) {
            checksum += b;
        }
        checksums.put(entry.getName(), Integer.toHexString(checksum));
    }

    private void writeStub() throws IOException {
        ZipEntry entry = new ZipEntry("META-INF/SKID.ONION");
        zos.putNextEntry(entry);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : checksums.entrySet()) {
            sb.append(e.getKey()).append(":").append(e.getValue()).append("\n");
        }
        zos.write(sb.toString().getBytes());
        zos.closeEntry();
    }
}
