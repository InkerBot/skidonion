package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.InstructionModifier;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.asm.accesses.AccessFlags;
import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.RandomUtils;
import tech.skidonion.obfuscator.utils.commons.Pair;
import tech.skidonion.obfuscator.value.impls.BooleanValue;
import tech.skidonion.obfuscator.value.impls.ModeValue;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class InvokeWrapperObfuscation extends Transformer {
    private final ModeValue package_mode = new ModeValue("package_mode", "random_existed", "root", "unique", "random_existed");
    private final BooleanValue inject_to_other_class = new BooleanValue("inject_to_other_class", true);
    private final List<Pair<ClassWrapper, MethodNode>> syntheticMethods = new ArrayList<>();

    public InvokeWrapperObfuscation(String name) {
        super(name);
        addSettings(package_mode, inject_to_other_class);
    }

    @Override
    public void transform() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        new ArrayList<>(getClassWrappers()).stream().filter(this::match).forEach(cw -> {
            removeAnnotation(cw);
            if (cw.getAccess().isInterface()) return;

            ClassNode node = cw.getClassNode();
            node.access = new AccessFlags(node.access).setPublic().getFlags();

            ClassWrapper target;
            if (!inject_to_other_class.isEnable()) {
                ClassNode targetNode = new ClassNode();

                String packageName = null;

                if (package_mode.is("root")) {
                    packageName = "";
                } else if (package_mode.is("unique")) {
                    packageName = obfuscator.packageDictionaries.computeIfAbsent("", name -> obfuscator.getDictionary().copy()).nextUniqueString() + "/";
                } else if (package_mode.is("random_existed")) {
                    List<ClassWrapper> wrappers = new ArrayList<>(getClassWrappers());
                    packageName = wrappers.get(RandomUtils.getRandomInt(wrappers.size())).getPackageName();
                }
                Dictionary classDictionary = obfuscator.classesDictionaries.computeIfAbsent(packageName, name -> obfuscator.getDictionary().copy());

                String name = packageName + classDictionary.nextUniqueString();
                targetNode.visit(V1_8, ACC_PUBLIC, name, null, "java/lang/Object", null);
                target = injectClass(targetNode);
            } else {
                List<ClassWrapper> wrappers = new ArrayList<>(getClassWrappers());
                target = wrappers.get(RandomUtils.getRandomInt(wrappers.size()));
            }

            for (MethodNode method : node.methods) {
                method.access = new AccessFlags(method.access).setPublic().getFlags();
            }

            for (FieldNode field : node.fields) {
                field.access = new AccessFlags(field.access).setPublic().getFlags();
            }

            for (MethodNode method : node.methods) {

                InstructionModifier modifier = new InstructionModifier();

                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                        ClassWrapper wrapper = obfuscator.classes.get(methodInsnNode.owner);
                        if (wrapper == null) continue;
                        MethodNode targetMethod = wrapper.getMethod(methodInsnNode.name, methodInsnNode.desc);
                        if (targetMethod == null) continue;
                        AccessFlags flag = new AccessFlags(targetMethod.access);
                        if (flag.isPrivate() || flag.isProtected())
                            continue;

                        if (methodInsnNode.getOpcode() == INVOKESTATIC) {
                            String methodName = inject_to_other_class.isEnable() ? target.generateRandomMethodName() : target.getMethodDictionary().nextUniqueString();
                            MethodNode methodNode = createStaticMethod(methodInsnNode, methodName);

                            syntheticMethods.add(new Pair<>(target, methodNode));
                            modifier.replace(instruction, new MethodInsnNode(INVOKESTATIC, target.getClassNode().name, methodName, methodInsnNode.desc, false));

                            counter.incrementAndGet();
                        } else if (methodInsnNode.getOpcode() == INVOKEVIRTUAL) {
                            String methodName = inject_to_other_class.isEnable() ? target.generateRandomMethodName() : target.getMethodDictionary().nextUniqueString();
                            MethodNode methodNode = createVirtualMethod(methodInsnNode, methodName);

                            syntheticMethods.add(new Pair<>(target, methodNode));
                            modifier.replace(instruction, new MethodInsnNode(INVOKESTATIC, target.getClassNode().name, methodName, methodNode.desc, false));

                            counter.incrementAndGet();
                        }
                    } else if (instruction instanceof FieldInsnNode) {
                        FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;

                        ClassWrapper wrapper = obfuscator.classes.get(fieldInsnNode.owner);
                        if (wrapper == null) continue;
                        FieldNode targetField = wrapper.getField(fieldInsnNode.name, fieldInsnNode.desc);
                        if (targetField == null) continue;
                        if (Modifier.isPrivate(targetField.access) || Modifier.isProtected(targetField.access))
                            continue;

                        if (fieldInsnNode.getOpcode() == GETSTATIC) {
                            String methodName = inject_to_other_class.isEnable() ? target.generateRandomMethodName() : target.getMethodDictionary().nextUniqueString();
                            MethodNode methodNode = createGetStaticMethod(fieldInsnNode, methodName);
                            syntheticMethods.add(new Pair<>(target, methodNode));
                            modifier.replace(instruction, new MethodInsnNode(INVOKESTATIC, target.getClassNode().name, methodName, methodNode.desc, false));
                            counter.incrementAndGet();
                        } else if (fieldInsnNode.getOpcode() == PUTSTATIC) {
                            if (fieldInsnNode.owner.equals(node.name)) {
                                for (FieldNode fieldNode : node.fields) {
                                    if (fieldInsnNode.name.equals(fieldNode.name)) {
                                        boolean isFinal = (fieldNode.access & ACC_FINAL) != 0;
                                        if (!isFinal) {
                                            String methodName = inject_to_other_class.isEnable() ? target.generateRandomMethodName() : target.getMethodDictionary().nextUniqueString();
                                            MethodNode methodNode = createPutStaticMethod(fieldInsnNode, methodName);
                                            syntheticMethods.add(new Pair<>(target, methodNode));
                                            modifier.replace(instruction, new MethodInsnNode(INVOKESTATIC, target.getClassNode().name, methodName, methodNode.desc, false));
                                        }
                                    }
                                }
                            } else {
                                String methodName = inject_to_other_class.isEnable() ? target.generateRandomMethodName() : target.getMethodDictionary().nextUniqueString();
                                MethodNode methodNode = createPutStaticMethod(fieldInsnNode, methodName);
                                syntheticMethods.add(new Pair<>(target, methodNode));
                                modifier.replace(instruction, new MethodInsnNode(INVOKESTATIC, target.getClassNode().name, methodName, methodNode.desc, false));
                            }
                            counter.incrementAndGet();
                        } else if (fieldInsnNode.getOpcode() == GETFIELD) {
                            if (!method.name.equals("<init>")) {
                                String methodName = inject_to_other_class.isEnable() ? target.generateRandomMethodName() : target.getMethodDictionary().nextUniqueString();
                                MethodNode methodNode = createGetFieldMethod(fieldInsnNode, methodName);
                                syntheticMethods.add(new Pair<>(target, methodNode));
                                modifier.replace(instruction, new MethodInsnNode(INVOKESTATIC, target.getClassNode().name, methodName, methodNode.desc, false));
                                counter.incrementAndGet();
                            }
                        } else if (fieldInsnNode.getOpcode() == PUTFIELD) {
                            if (!method.name.equals("<init>")) {
                                String methodName = inject_to_other_class.isEnable() ? target.generateRandomMethodName() : target.getMethodDictionary().nextUniqueString();
                                MethodNode methodNode = createPutFieldMethod(fieldInsnNode, methodName);

                                syntheticMethods.add(new Pair<>(target, methodNode));
                                modifier.replace(instruction, new MethodInsnNode(INVOKESTATIC, target.getClassNode().name, methodName, methodNode.desc, false));

                                counter.incrementAndGet();
                            }
                        }
                    }
                }
                modifier.apply(method);
            }
        });

        for (Pair<ClassWrapper, MethodNode> pair : syntheticMethods) {
            pair.getFirst().addMethod(pair.getSecond());
        }


        PhantomShield.INFO(PhantomShield.TRANSLATION("phantom-shield-x.invoke-wrapper.wrapped"), counter.get());
    }

    @Override
    public void preprocess() throws Exception {

    }

    @Override
    public String annotation() {
        return null;
    }

    private MethodNode createStaticMethod(MethodInsnNode methodInsnNode, String methodName) {
        String desc = methodInsnNode.desc;
        MethodNode methodNode = new MethodNode(ACC_PUBLIC | ACC_STATIC, methodName, desc, null, null);
        Type returnType = Type.getReturnType(desc);

        visitArgs(0, Type.getArgumentTypes(desc), methodNode);
        methodNode.visitMethodInsn(INVOKESTATIC, methodInsnNode.owner, methodInsnNode.name, desc, methodInsnNode.itf);
        visitReturn(returnType, methodNode);

        return methodNode;
    }

    private MethodNode createVirtualMethod(MethodInsnNode methodInsnNode, String methodName) {
        Type[] types = Type.getArgumentTypes(methodInsnNode.desc);
        Type[] desc = new Type[types.length + 1];

        for (int i = 0; i < desc.length; i++) {
            if (i == 0) {
                desc[i] = Type.getObjectType(methodInsnNode.owner);
            } else {
                desc[i] = types[i - 1];
            }
        }

        String methodDesc = Type.getMethodDescriptor(Type.getReturnType(methodInsnNode.desc), desc);
        MethodNode methodNode = new MethodNode(ACC_PUBLIC | ACC_STATIC, methodName, methodDesc, null, null);

        methodNode.visitVarInsn(ALOAD, 0);
        visitArgs(1, types, methodNode);
        methodNode.visitMethodInsn(INVOKEVIRTUAL, methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, methodInsnNode.itf);
        visitReturn(Type.getReturnType(methodInsnNode.desc), methodNode);

        return methodNode;
    }

    private MethodNode createGetStaticMethod(FieldInsnNode fieldInsnNode, String methodName) {
        Type type = Type.getType(fieldInsnNode.desc);
        String methodDescriptor = Type.getMethodDescriptor(type);
        MethodNode methodNode = new MethodNode(ACC_PUBLIC | ACC_STATIC, methodName, methodDescriptor, null, null);

        methodNode.visitFieldInsn(GETSTATIC, fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc);
        visitReturn(type, methodNode);

        return methodNode;
    }

    private MethodNode createPutStaticMethod(FieldInsnNode fieldInsnNode, String methodName) {
        Type type = Type.getType(fieldInsnNode.desc);
        String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, type);
        MethodNode methodNode = new MethodNode(ACC_PUBLIC | ACC_STATIC, methodName, methodDescriptor, null, null);

        visitArgs(0, new Type[]{type}, methodNode);
        methodNode.visitFieldInsn(PUTSTATIC, fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc);
        methodNode.visitInsn(RETURN);

        return methodNode;
    }

    private MethodNode createGetFieldMethod(FieldInsnNode fieldInsnNode, String methodName) {
        Type type = Type.getType(fieldInsnNode.desc);
        Type objectType = Type.getObjectType(fieldInsnNode.owner);
        String methodDescriptor = Type.getMethodDescriptor(type, objectType);
        MethodNode methodNode = new MethodNode(ACC_PUBLIC | ACC_STATIC, methodName, methodDescriptor, null, null);

        visitArgs(0, new Type[]{objectType}, methodNode);
        methodNode.visitFieldInsn(GETFIELD, fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc);
        visitReturn(type, methodNode);

        return methodNode;
    }

    private MethodNode createPutFieldMethod(FieldInsnNode fieldInsnNode, String methodName) {
        Type type = Type.getType(fieldInsnNode.desc);
        Type objectType = Type.getObjectType(fieldInsnNode.owner);
        String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, objectType, type);
        MethodNode methodNode = new MethodNode(ACC_PUBLIC | ACC_STATIC, methodName, methodDescriptor, null, null);

        visitArgs(0, new Type[]{objectType, type}, methodNode);
        methodNode.visitFieldInsn(PUTFIELD, fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc);
        methodNode.visitInsn(RETURN);

        return methodNode;
    }

    private static void visitArgs(int offset, Type[] types, MethodNode methodNode) {
        int index = offset;

        for (Type type : types) {
            int loadOpcode = ASMUtils.getVarOpcode(type, false);
            methodNode.visitVarInsn(loadOpcode, index);

            index += (type.getSize() == 2) ? 2 : 1;
        }
    }

    private static void visitReturn(Type type, MethodNode methodNode) {
        int returnOpcode = ASMUtils.getReturnOpcode(type);
        methodNode.visitInsn(returnOpcode);
    }

}