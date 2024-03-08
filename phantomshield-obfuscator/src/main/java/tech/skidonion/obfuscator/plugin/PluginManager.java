package tech.skidonion.obfuscator.plugin;


import tech.skidonion.sdk.JavaPlugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;

import static tech.skidonion.obfuscator.PhantomShield.*;

public class PluginManager {
    private final File pluginPath;
    private static final String link = "https://example.com";

    public PluginManager() {
        this.pluginPath = new File("plugins");
    }

    public void init() {
        if (!pluginPath.exists() && !pluginPath.mkdir()) {
            throw new RuntimeException("Unable to create plugins directory");
        }

        INFO("Loading plugins form {}.",pluginPath.getAbsolutePath());

        File[] files = pluginPath.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".jar")) {
                    loadPlugin(file);
                }
            }
        }
    }

    private void loadPlugin(File input) {
        try {
            List<Class<?>> classList = new ArrayList<>();
            try(URLClassLoader loader = new URLClassLoader(new URL[]{input.toURI().toURL()}, PluginManager.class.getClassLoader())) {
                try (JarFile jarFile = new JarFile(input)) {
                    for (java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                        java.util.jar.JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            try {
                                String className = entry.getName().replace("/", ".").replaceAll("\\.class$", "");
                                Class<?> clazz = loader.loadClass(className);
                                classList.add(clazz);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                                ERROR("Error loading class from jar: {}", e.getMessage());
                            }
                        }
                    }
                }
                processLoadedClasses(classList,input);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            ERROR("Error loading plugin from jar: {}", e.getMessage());
        }
    }

    private void processLoadedClasses(List<Class<?>> classList,File file) {
        AtomicBoolean hasMainClass = new AtomicBoolean(false);
        classList.forEach(clazz -> {
            if(clazz.isAnnotationPresent(JavaPlugin.class)){
                hasMainClass.set(true);
                JavaPlugin metaInfo = clazz.getAnnotation(JavaPlugin.class);

                INFO("Loading plugin {} {}.",metaInfo.name(),metaInfo.version());
                try {
                    clazz.getConstructor().newInstance();
                } catch (Throwable e) {
                    e.printStackTrace();
                    ERROR("An error occurred during {} plugin loading process.",metaInfo.name());
                    ERROR("If you are the developer of this plugin, please refer to our documentation at {} to resolve this issue.", link);
                }
            }
        });
        if(!hasMainClass.get()){
            ERROR("Plugin {} failed to load due to it lacks the @JavaPlugin annotation.", file.getAbsolutePath());
            ERROR("If you are the developer of this plugin, please refer to our documentation at {} to resolve this issue.",link);
        }
    }
}
