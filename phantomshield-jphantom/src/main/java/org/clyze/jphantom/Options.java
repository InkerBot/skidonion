package org.clyze.jphantom;

import java.nio.file.*;

import org.objectweb.asm.Opcodes;

public class Options {
    private final static FileSystem fs = FileSystems.getDefault();
    private final static Options INSTANCE = new Options();
    public static final int ASM_VER = Opcodes.ASM9;

    public static Options V() {
        return INSTANCE;
    }

    protected Options() {
    }


    private boolean softFail = true;

    private int javaVersion = 8;


    public boolean isSoftFail() {
        return softFail;
    }

    public void setSoftFail(boolean softFail) {
        this.softFail = softFail;
    }

    public int getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(int javaVersion) {
        this.javaVersion = javaVersion;
    }

}
