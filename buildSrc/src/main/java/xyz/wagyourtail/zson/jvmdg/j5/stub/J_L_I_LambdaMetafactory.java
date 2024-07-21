package xyz.wagyourtail.zson.jvmdg.j5.stub;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.util.Arrays;

public class J_L_I_LambdaMetafactory {


    @Modify(
        ref = @Ref(
            value = "java/lang/invoke/LambdaMetafactory",
            member = "metafactory",
            desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
        )
    )
    public static void stubMetafactory(MethodNode mn, int idx, ClassNode selfClass) {
        AbstractInsnNode insn = mn.instructions.get(idx);
        if (insn.getOpcode() != Opcodes.INVOKEDYNAMIC) return;
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
        if (Type.getArgumentCount(indy.desc) == 0) {
            for (FieldNode fn : selfClass.fields) {
                if (fn instanceof IndyFieldNode) {
                    if (fn.desc.equals(Type.getReturnType(indy.desc).getDescriptor()) && Arrays.equals(((IndyFieldNode) fn).bsmArgs, indy.bsmArgs)) {
                        mn.instructions.set(insn, new FieldInsnNode(Opcodes.GETSTATIC, selfClass.name, fn.name, fn.desc));
                        return;
                    }
                }
            }
        } else {
            for (MethodNode fn : selfClass.methods) {
                if (fn instanceof IndyMethodNode) {
                    if (fn.desc.equals(indy.desc) && Arrays.equals(((IndyMethodNode) fn).bsmArgs, indy.bsmArgs)) {
                        mn.instructions.set(insn, new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            selfClass.name,
                            fn.name,
                            fn.desc,
                            false
                        ));
                        return;
                    }
                }
            }
        }
        // create IndyMethodNode and bootstrapping
        mn.instructions.set(indy, createIndy(indy, selfClass));
    }

    public static AbstractInsnNode createIndy(InvokeDynamicInsnNode indy, ClassNode selfClass) {
        Type returnType = Type.getReturnType(indy.desc);
        Type[] args = Type.getArgumentTypes(indy.desc);

        Type bridge = (Type) indy.bsmArgs[0];
        Handle invokedMethod = (Handle) indy.bsmArgs[1];
        Type invokedType = (Type) indy.bsmArgs[2];
        // create bootstrapping of indy callsite
        // find <clinit> method
        MethodNode clinit = null;
        for (MethodNode mn : selfClass.methods) {
            if ("<clinit>".equals(mn.name)) {
                clinit = mn;
                break;
            }
        }
        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.visitCode();
            clinit.visitInsn(Opcodes.RETURN);
            clinit.visitMaxs(0, 0);
            clinit.visitEnd();
            selfClass.methods.add(clinit);
        }
        // insert to beginning of <clinit>
        InsnList l = new InsnList();
        // get lookup
        l.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "java/lang/invoke/MethodHandles",
            "lookup",
            "()Ljava/lang/invoke/MethodHandles$Lookup;",
            false
        ));
        // get name
        l.add(new LdcInsnNode(indy.name));
        // get desc
        l.add(methodDescToMethodType(Type.getMethodType(indy.desc)));
        // get bridge type
        l.add(methodDescToMethodType(bridge));
        // get invoked method, except we can't ldc the handle, so we have to look it up
        // get lookup
        l.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "java/lang/invoke/MethodHandles",
            "lookup",
            "()Ljava/lang/invoke/MethodHandles$Lookup;",
            false
        ));
        l.add(new LdcInsnNode(Type.getObjectType(invokedMethod.getOwner())));
        if (invokedMethod.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
            l.add(methodDescToMethodType(Type.getMethodType(invokedMethod.getDesc())));
            l.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                "findConstructor",
                "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false
            ));
        } else if (invokedMethod.getTag() == Opcodes.H_INVOKESPECIAL) {
            l.add(new LdcInsnNode(invokedMethod.getName()));
            l.add(methodDescToMethodType(Type.getMethodType(invokedMethod.getDesc())));
            l.add(new LdcInsnNode(Type.getObjectType(selfClass.name)));
            l.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                "findSpecial",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
                false
            ));
        } else {
            l.add(new LdcInsnNode(invokedMethod.getName()));
            l.add(methodDescToMethodType(Type.getMethodType(invokedMethod.getDesc())));
            l.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                invokedMethod.getTag() == Opcodes.H_INVOKESTATIC ? "findStatic" : "findVirtual",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false
            ));
        }
        // get invoked type
        l.add(methodDescToMethodType(invokedType));
        // call metafactory
        l.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            false
        ));
        // call getTarget
        l.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/invoke/CallSite",
            "getTarget",
            "()Ljava/lang/invoke/MethodHandle;",
            false
        ));
        if (args.length != 0) {
            // create a field to store the handle
            FieldNode fn = new FieldNode(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, "jvmdg$indy$" + selfClass.fields.size(), "Ljava/lang/invoke/MethodHandle;", null, null);
            selfClass.fields.add(fn);
            l.add(new FieldInsnNode(Opcodes.PUTSTATIC, selfClass.name, fn.name, fn.desc));
            // create a method to construct using the handle
            MethodNode mn = new IndyMethodNode(indy, "jvmdg$indy$" + selfClass.methods.size());
            mn.visitCode();
            mn.visitFieldInsn(Opcodes.GETSTATIC, selfClass.name, fn.name, fn.desc);
            for (int i = 0, j = 0; i < args.length; i++) {
                mn.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), j);
                j += args[i].getSize();
            }
            // invokeExact
            mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", indy.desc, false);
            mn.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
            mn.visitEnd();
            selfClass.methods.add(mn);
            clinit.instructions.insert(l);
            return new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                selfClass.name,
                mn.name,
                mn.desc,
                false
            );
        } else {
            // call invokeExact
            l.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandle",
                "invokeExact",
                indy.desc,
                false
            ));
            // store to new field
            IndyFieldNode fn = new IndyFieldNode(indy, "jvmdg$indy$" + selfClass.fields.size());
            selfClass.fields.add(fn);
            l.add(new FieldInsnNode(Opcodes.PUTSTATIC, selfClass.name, fn.name, fn.desc));
            clinit.instructions.insert(l);
            return new FieldInsnNode(Opcodes.GETSTATIC, selfClass.name, fn.name, fn.desc);
        }
    }

    public static String getBoxTypeForPrimitive(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
                return "java/lang/Boolean";
            case Type.BYTE:
                return "java/lang/Byte";
            case Type.CHAR:
                return "java/lang/Character";
            case Type.SHORT:
                return "java/lang/Short";
            case Type.INT:
                return "java/lang/Integer";
            case Type.LONG:
                return "java/lang/Long";
            case Type.FLOAT:
                return "java/lang/Float";
            case Type.DOUBLE:
                return "java/lang/Double";
            default:
                return null;
        }
    }

    public static InsnList methodDescToMethodType(Type desc) {
        InsnList l = new InsnList();
        Type returnType = desc.getReturnType();
        Type[] args = desc.getArgumentTypes();
        if (returnType.getSort() < Type.ARRAY) {
            l.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                getBoxTypeForPrimitive(returnType),
                "TYPE",
                "Ljava/lang/Class;"
            ));
        } else {
            l.add(new LdcInsnNode(returnType));
        }
        l.add(new LdcInsnNode(args.length));
        l.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Class"));
        for (int i = 0; i < args.length; i++) {
            l.add(new InsnNode(Opcodes.DUP));
            l.add(new LdcInsnNode(i));
            if (args[i].getSort() < Type.ARRAY) {
                l.add(new FieldInsnNode(
                    Opcodes.GETSTATIC,
                    getBoxTypeForPrimitive(args[i]),
                    "TYPE",
                    "Ljava/lang/Class;"
                ));
            } else {
                l.add(new LdcInsnNode(args[i]));
            }
            l.add(new InsnNode(Opcodes.AASTORE));
        }
        l.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "java/lang/invoke/MethodType",
            "methodType",
            "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;",
            false
        ));
        return l;
    }

    public static class IndyFieldNode extends FieldNode {
        Object[] bsmArgs;

        public IndyFieldNode(InvokeDynamicInsnNode indy, String name) {
            super(Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, name, Type.getReturnType(indy.desc).getDescriptor(), null, null);
            this.bsmArgs = indy.bsmArgs;
        }
    }

    public static class IndyMethodNode extends MethodNode {
        Object[] bsmArgs;

        public IndyMethodNode(InvokeDynamicInsnNode indy, String name) {
            super(Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, name, indy.desc, null, null);
            this.bsmArgs = indy.bsmArgs;
        }
    }

}
