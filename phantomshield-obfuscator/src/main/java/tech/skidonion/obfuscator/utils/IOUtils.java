package tech.skidonion.obfuscator.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;

/**
 * IO utilities.
 */
public class IOUtils {
    /**
     * Creates a byte array from a given {@link InputStream}.
     *
     * @param in {@link InputStream} to convert to a byte array.
     * @return a byte array from the inputted
     */
    public static byte[] toByteArray(InputStream in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transfer(in, out);
            return out.toByteArray();
        } catch (IOException ioe) {
            ERROR("", ioe);
            throw new RuntimeException(ioe);
        }
    }


    public static void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        for (int r = in.read(buffer, 0, 4096); r != -1; r = in.read(buffer, 0, 4096)) {
            out.write(buffer, 0, r);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Stream<T> reverse(Stream<T> input) {
        Object[] temp = input.toArray();
        return (Stream<T>) IntStream.range(0, temp.length)
                .mapToObj(i -> temp[temp.length - i - 1]);
    }


    public static String writeStreamToString(InputStream stream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.transfer(stream, baos);
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Map<String, byte[]> readJarResources(String path) {
        try (InputStream stream = ASMUtils.class.getResourceAsStream(path); ZipInputStream zip = new ZipInputStream(Objects.requireNonNull(stream));) {
            Map<String, byte[]> map = new HashMap<>();
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && !entry.getName().endsWith(".class") && !"META-INF/MANIFEST.MF".equals(entry.getName())) {
                    map.put(entry.getName(), IOUtils.toByteArray(zip));
                }
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
