#include "native_jvm.hpp"
#include "native_jvm_output.hpp"
#include "string_pool.hpp"

$includes

namespace native_jvm {

    typedef void (* reg_method)(JNIEnv *,jclass);

    reg_method reg_methods[$class_count];

    void register_for_class(JNIEnv *env, jclass, jint id, jclass clazz) {
        reg_methods[id](env, clazz);
    }

    void prepare_lib(JNIEnv *env) {
        utils::init_utils(env);
        if (env->ExceptionCheck())
            return;

        char* string_pool = string_pool::get_pool();

$register_code

        if (env->ExceptionCheck())
            return;

        char method_name[] = "___";
        char method_desc[] = "(ILjava/lang/Class;)V";
        JNINativeMethod loader_methods[] = {
            { (char *) method_name, (char *) method_desc, (void *)&register_for_class }
        };
        env->RegisterNatives(env->FindClass("$native_dir/Loader"), loader_methods, 1);
    }
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    vm->GetEnv((void **) &env, JNI_VERSION_1_8);

// FIXME Use jString instead of std::string
//    jclass classLoader = env->FindClass("java/lang/ClassLoader");
//    jmethodID getSystemClassLoader = env->GetStaticMethodID(classLoader, "getSystemClassLoader",
//                                                           "()Ljava/lang/ClassLoader;");
//    jobject systemClassLoader = env->CallStaticObjectMethod(classLoader, getSystemClassLoader);
//    jclass urlClassLoader = env->FindClass("java/net/URLClassLoader");
//    jmethodID getURLs = env->GetMethodID(urlClassLoader, "getURLs", "()[Ljava/net/URL;");
//    jobjectArray urls = (jobjectArray) env->CallObjectMethod(systemClassLoader, getURLs);
//    jsize urlCount = env->GetArrayLength(urls);
//    for (int i = 0; i < urlCount; i++) {
//        jobject url = env->GetObjectArrayElement(urls, i);
//        jclass urlClass = env->FindClass("java/net/URL");
//        jmethodID toExternalForm = env->GetMethodID(urlClass, "toExternalForm", "()Ljava/lang/String;");
//        jstring urlString = (jstring) env->CallObjectMethod(url, toExternalForm);
//        const char *urlCString = env->GetStringUTFChars(urlString, 0);
//        if (strstr(urlCString, ".jar")) {
//            jclass jarFileClass = env->FindClass("java/util/jar/JarFile");
//            jmethodID jarFileConstructor = env->GetMethodID(jarFileClass, "<init>", "(Ljava/lang/String;)V");
//            jobject jarFile = env->NewObject(jarFileClass, jarFileConstructor, urlString);
//            jmethodID getEntry = env->GetMethodID(jarFileClass, "getEntry",
//                                                  "(Ljava/lang/String;)Ljava/util/jar/JarEntry;");
//            jstring onionString = env->NewStringUTF("META-INF/SKID.ONION");
//            jobject onionEntry = env->CallObjectMethod(jarFile, getEntry, onionString);
//
//            if (onionEntry != nullptr) {
//                // Read the checksums into a map
//                jclass inputStreamClass = env->FindClass("java/io/InputStream");
//                jmethodID getInputStream = env->GetMethodID(jarFileClass, "getInputStream",
//                                                            "(Ljava/util/zip/ZipEntry;)Ljava/io/InputStream;");
//                jobject inputStream = env->CallObjectMethod(jarFile, getInputStream, onionEntry);
//                jclass scannerClass = env->FindClass("java/util/Scanner");
//                jmethodID scannerConstructor = env->GetMethodID(scannerClass, "<init>", "(Ljava/io/InputStream;)V");
//                jobject scanner = env->NewObject(scannerClass, scannerConstructor, inputStream);
//                jmethodID hasNextLine = env->GetMethodID(scannerClass, "hasNextLine", "()Z");
//                jmethodID nextLine = env->GetMethodID(scannerClass, "nextLine", "()Ljava/lang/String;");
//                std::map <std::string, std::string> checksums;
//                while (env->CallBooleanMethod(scanner, hasNextLine)) {
//                    jstring line = (jstring) env->CallObjectMethod(scanner, nextLine);
//                    const char *lineCString = env->GetStringUTFChars(line, 0);
//                    char *separator = strchr(lineCString, ':');
//                    if (separator != nullptr) {
//                        *separator = '\0';
//                        checksums[lineCString] = separator + 1;
//                    }
//                    env->ReleaseStringUTFChars(line, lineCString);
//                }
//                jmethodID entries = env->GetMethodID(jarFileClass, "entries", "()Ljava/util/Enumeration;");
//                jobject enumeration = env->CallObjectMethod(jarFile, entries);
//                jclass enumerationClass = env->FindClass("java/util/Enumeration");
//                jmethodID hasMoreElements = env->GetMethodID(enumerationClass, "hasMoreElements", "()Z");
//                jmethodID nextElement = env->GetMethodID(enumerationClass, "nextElement", "()Ljava/lang/Object;");
//                while (env->CallBooleanMethod(enumeration, hasMoreElements)) {
//                    jobject entry = env->CallObjectMethod(enumeration, nextElement);
//                    jclass jarEntryClass = env->FindClass("java/util/jar/JarEntry");
//                    jmethodID getName = env->GetMethodID(jarEntryClass, "getName", "()Ljava/lang/String;");
//                    jstring name = (jstring) env->CallObjectMethod(entry, getName);
//                    const char *nameCString = env->GetStringUTFChars(name, 0);
//                    if (strcmp(nameCString, "META-INF/SKID.ONION") == 0) {
//                        env->ReleaseStringUTFChars(name, nameCString);
//                        continue;
//                    }
//                    inputStream = env->CallObjectMethod(jarFile, getInputStream, entry);
//                    scanner = env->NewObject(scannerClass, scannerConstructor, inputStream);
//                    jmethodID hasNextByte = env->GetMethodID(scannerClass, "hasNextByte", "()Z");
//                    jmethodID nextByte = env->GetMethodID(scannerClass, "nextByte", "()B");
//                    int checksum = 0;
//                    while (env->CallBooleanMethod(scanner, hasNextByte)) {
//                        jbyte b = env->CallByteMethod(scanner, nextByte);
//                        checksum += b;
//                    }
//                    // Compare the checksum with the corresponding checksum from the map
//                    if (checksums[nameCString] != std::to_string(checksum)) {
//                        // The checksums do not match
//                        env->ReleaseStringUTFChars(name, nameCString);
//                        return JNI_ERR;
//                    }
//                    env->ReleaseStringUTFChars(name, nameCString);
//                }
//            }
//        }
//        env->ReleaseStringUTFChars(urlString, urlCString);
//    }

    native_jvm::prepare_lib(env);
    return JNI_VERSION_1_8;
}
