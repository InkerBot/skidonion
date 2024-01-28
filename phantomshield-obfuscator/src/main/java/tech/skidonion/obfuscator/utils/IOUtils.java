package tech.skidonion.obfuscator.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

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
            byte[] buffer = new byte[1024];
            while (in.available() > 0) {
                int data = in.read(buffer);
                out.write(buffer, 0, data);
            }

            in.close();
            out.close();
            return out.toByteArray();
        } catch (IOException ioe) {
            ioe.printStackTrace();
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


    public static void writeEntry(JarFile f, ZipOutputStream out, JarEntry e) throws IOException {
        out.putNextEntry(new JarEntry(e.getName()));
        try (InputStream in = f.getInputStream(e)) {
            transfer(in, out);
        }
        out.closeEntry();
    }

    public static void writeEntry(ZipOutputStream out, String entryName, byte[] data) throws IOException {
        out.putNextEntry(new JarEntry(entryName));
        out.write(data, 0, data.length);
        out.closeEntry();
    }
}
