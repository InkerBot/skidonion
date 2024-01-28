package tech.skidonion.obfuscator.utils;


import tech.skidonion.obfuscator.PhantomShield;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class FileUtils {
    /**
     * Renames an existing file to EXISTING-FILE.jar.BACKUP-X.
     *
     * @param existing existing file to rename.
     * @return the new name of the existing name.
     */
    public static String renameExistingFile(File existing) {
        int i = 0;

        while (true) {
            i++;
            String newName = existing.getAbsolutePath() + ".BACKUP-" + String.valueOf(i);
            File backUpName = new File(newName);
            if (!backUpName.exists()) {
                existing.renameTo(backUpName);
                existing.delete();
                return newName;
            }
        }

    }

    /**
     * Searches sub directories for libraries
     *
     * @param file      should be directory
     * @param libraries all libraries collected.
     * @author Richard Xing
     */
    public static void getSubDirectoryFiles(File file, List<File> libraries) {
        if (!file.isFile() && file.listFiles() != null) {
            Stream.of(file.listFiles()).forEach(f -> {
                // 输出元素名称

                if (f.isDirectory()) {
                    getSubDirectoryFiles(f, libraries);
                } else {
                    if (f.getName().toLowerCase().endsWith(".jar")) {
                        //System.out.println(fileLists[i].getConfigName());
                        libraries.add(f);
                    }
                }
            });
        }
    }


    public static String readResource(String filePath) {
        try (InputStream in = PhantomShield.class.getClassLoader().getResourceAsStream(filePath)) {
            return IOUtils.writeStreamToString(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copyResource(String from, Path to) throws IOException {
        try (InputStream in = PhantomShield.class.getClassLoader().getResourceAsStream(from)) {
            Objects.requireNonNull(in, "Can't copy resource " + from);
            Files.copy(in, to.resolve(Paths.get(from).getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
