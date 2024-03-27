package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.internals;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
public class VerifyUtils$1Dump implements Opcodes {

public static byte[] dump () throws Exception {

ClassWriter classWriter = new ClassWriter(0);
FieldVisitor fieldVisitor;
RecordComponentVisitor recordComponentVisitor;
MethodVisitor methodVisitor;
AnnotationVisitor annotationVisitor0;

classWriter.visit(V1_8, ACC_FINAL | ACC_SUPER, "tech/skidonion/verification/utils/VerifyUtils$1", "Ljava/util/HashMap<Ljava/lang/String;Ljava/lang/String;>;", "java/util/HashMap", null);

classWriter.visitSource("VerifyUtils.java", null);

classWriter.visitOuterClass("tech/skidonion/verification/utils/VerifyUtils", "genericHeader", "()Ljava/util/Map;");

classWriter.visitInnerClass("tech/skidonion/verification/utils/VerifyUtils$1", null, null, ACC_STATIC);

{
methodVisitor = classWriter.visitMethod(0, "<init>", "()V", null, null);
methodVisitor.visitCode();
Label label0 = new Label();
methodVisitor.visitLabel(label0);
methodVisitor.visitLineNumber(322, label0);
methodVisitor.visitVarInsn(ALOAD, 0);
methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
Label label1 = new Label();
methodVisitor.visitLabel(label1);
methodVisitor.visitLineNumber(324, label1);
methodVisitor.visitMethodInsn(INVOKESTATIC, "tech/skidonion/verification/utils/VerifyUtils", "access$000", "()Ljava/lang/String;", false);
Label label2 = new Label();
methodVisitor.visitJumpInsn(IFNULL, label2);
methodVisitor.visitVarInsn(ALOAD, 0);
methodVisitor.visitLdcInsn("verify-token");
methodVisitor.visitMethodInsn(INVOKESTATIC, "tech/skidonion/verification/utils/VerifyUtils", "access$000", "()Ljava/lang/String;", false);
methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "tech/skidonion/verification/utils/VerifyUtils$1", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
methodVisitor.visitInsn(POP);
methodVisitor.visitLabel(label2);
methodVisitor.visitLineNumber(325, label2);
methodVisitor.visitFrame(Opcodes.F_NEW, 1, new Object[] {"tech/skidonion/verification/utils/VerifyUtils$1"}, 0, new Object[] {});
methodVisitor.visitInsn(RETURN);
Label label3 = new Label();
methodVisitor.visitLabel(label3);
methodVisitor.visitLocalVariable("this", "Ltech/skidonion/verification/utils/VerifyUtils$1;", null, label0, label3, 0);
methodVisitor.visitMaxs(3, 1);
methodVisitor.visitEnd();
}
classWriter.visitEnd();

return classWriter.toByteArray();
}
}
