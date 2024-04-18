import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ___ {
    public static native void ___(int index, Class<?> clazz);

    static {
        System.out.println("[skidonion] loading native library...");

        String osName = System.getProperty("os.name").toLowerCase();
        String platform = System.getProperty("os.arch").toLowerCase();

        StringBuilder libName = new StringBuilder("PhantomShieldX");
        if (platform.contains("x86_64") || platform.contains("amd64")) {
            libName.append("64");
        } else if (platform.contains("aarch64")) {
            libName.append("ARM64");
        } else if (platform.contains("x86")) {
            libName.append("32");
        }
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            libName.insert(0, "lib");
            libName.append(".so");
        } else if (osName.contains("win")) {
            libName.append(".dll");
        } else if (osName.contains("mac")) {
            libName.insert(0, "lib");
            libName.append(".dylib");
        } else {
            libName.insert(0, "lib");
            libName.append(".so");
        }

        String libFileName = String.format("/%s/%s", ___.class.getPackage().getName().replace(".", "/"), libName);

        File libFile;
        try {
            libFile = File.createTempFile("lib", null);
            libFile.deleteOnExit();
            if (!libFile.exists()) {
                throw new IOException();
            }
        } catch (IOException iOException) {
            throw new UnsatisfiedLinkError("Failed to create temp file");
        }
        byte[] arrayOfByte = new byte[2048];
        try {
            InputStream inputStream = ___.class.getResourceAsStream(libFileName);
            if (inputStream == null) {
                throw new UnsatisfiedLinkError(String.format("Failed to open lib file: %s", libFileName));
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(libFile);
                try {
                    int size;
                    while ((size = inputStream.read(arrayOfByte)) != -1) {
                        fileOutputStream.write(arrayOfByte, 0, size);
                    }
                    fileOutputStream.close();
                } catch (Throwable throwable) {
                    try {
                        fileOutputStream.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                    throw throwable;
                }
                inputStream.close();
            } catch (Throwable throwable) {
                try {
                    inputStream.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (IOException exception) {
            throw new UnsatisfiedLinkError(String.format("Failed to copy file: %s", exception.getMessage()));
        }
        System.load(libFile.getAbsolutePath());
    }
}
