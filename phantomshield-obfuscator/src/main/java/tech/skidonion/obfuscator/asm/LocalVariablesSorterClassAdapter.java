package tech.skidonion.obfuscator.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class LocalVariablesSorterClassAdapter extends ClassVisitor {

    LocalVariablesSorterClassAdapter(final int api, final ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new LocalVariablesSorter(api, access, descriptor, methodVisitor) {
        };
    }
}