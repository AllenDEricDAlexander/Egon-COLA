package top.egon.cola.component.bytecode.core.classfile;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import top.egon.cola.component.bytecode.api.architecture.DependencyKind;
import top.egon.cola.component.bytecode.api.architecture.LocationKind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AsmClassMetadataReader implements ClassMetadataReader {

    @Override
    public ClassMetadata read(String module, byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        MetadataVisitor visitor = new MetadataVisitor();
        reader.accept(visitor, ClassReader.SKIP_FRAMES);
        visitor.readConstantPool(reader);
        return visitor.toMetadata(module);
    }

    private static final class MetadataVisitor extends ClassVisitor {

        private final Set<String> interfaces = new LinkedHashSet<>();
        private final Set<String> annotations = new LinkedHashSet<>();
        private final Set<ClassDependency> dependencies = new LinkedHashSet<>();
        private String className;
        private String superName;
        private boolean interfaceType;

        private MetadataVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                String superName, String[] interfaces) {
            this.className = dotted(name);
            this.superName = dotted(superName);
            this.interfaceType = (access & Opcodes.ACC_INTERFACE) != 0;
            addInternal(superName, DependencyKind.EXTENDS, LocationKind.CLASS,
                    null, null, null, null);
            if (interfaces != null) {
                for (String interfaceName : interfaces) {
                    this.interfaces.add(dotted(interfaceName));
                    addInternal(interfaceName, DependencyKind.IMPLEMENTS, LocationKind.CLASS,
                            null, null, null, null);
                }
            }
            addSignature(signature, null, null, LocationKind.CLASS, null);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            annotations.add(Type.getType(descriptor).getClassName());
            addType(Type.getType(descriptor), DependencyKind.ANNOTATION, LocationKind.CLASS,
                    null, null, null, null, null);
            return annotationVisitor(null, null, LocationKind.CLASS, null);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                String signature, Object value) {
            addType(Type.getType(descriptor), DependencyKind.FIELD, LocationKind.FIELD,
                    name, descriptor, null, null, null);
            addSignature(signature, name, descriptor, LocationKind.FIELD, null);
            addConstant(value, DependencyKind.CONSTANT_POOL, LocationKind.FIELD,
                    name, descriptor, null);
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                    addType(Type.getType(annotationDescriptor), DependencyKind.ANNOTATION,
                            LocationKind.FIELD, name, descriptor, null, null, null);
                    return annotationVisitor(name, descriptor, LocationKind.FIELD, null);
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                String signature, String[] exceptions) {
            Type methodType = Type.getMethodType(descriptor);
            for (Type argumentType : methodType.getArgumentTypes()) {
                addType(argumentType, DependencyKind.PARAMETER, LocationKind.METHOD,
                        name, descriptor, null, null, null);
            }
            addType(methodType.getReturnType(), DependencyKind.RETURN, LocationKind.METHOD,
                    name, descriptor, null, null, null);
            if (exceptions != null) {
                for (String exception : exceptions) {
                    addInternal(exception, DependencyKind.THROWS, LocationKind.METHOD,
                            name, descriptor, null, null);
                }
            }
            addSignature(signature, name, descriptor, LocationKind.METHOD, null);
            return new DependencyMethodVisitor(name, descriptor);
        }

        private AnnotationVisitor annotationVisitor(String member, String descriptor,
                LocationKind locationKind, Integer lineNumber) {
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public void visit(String name, Object value) {
                    addConstant(value, DependencyKind.ANNOTATION, locationKind,
                            member, descriptor, lineNumber);
                }

                @Override
                public void visitEnum(String name, String enumDescriptor, String value) {
                    addType(Type.getType(enumDescriptor), DependencyKind.ANNOTATION,
                            locationKind, member, descriptor, null, null, lineNumber);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String name, String annotationDescriptor) {
                    addType(Type.getType(annotationDescriptor), DependencyKind.ANNOTATION,
                            locationKind, member, descriptor, null, null, lineNumber);
                    return annotationVisitor(member, descriptor, locationKind, lineNumber);
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    return annotationVisitor(member, descriptor, locationKind, lineNumber);
                }
            };
        }

        private void addSignature(String signature, String member, String descriptor,
                LocationKind locationKind, Integer lineNumber) {
            if (signature == null) {
                return;
            }
            new SignatureReader(signature).accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public void visitClassType(String name) {
                    addInternal(name, DependencyKind.SIGNATURE, locationKind,
                            member, descriptor, null, lineNumber);
                }
            });
        }

        private void addType(Type type, DependencyKind kind, LocationKind locationKind,
                String sourceMember, String sourceDescriptor, String targetMember,
                String targetDescriptor, Integer lineNumber) {
            if (type == null) {
                return;
            }
            switch (type.getSort()) {
                case Type.ARRAY -> addType(type.getElementType(), kind, locationKind,
                        sourceMember, sourceDescriptor, targetMember, targetDescriptor, lineNumber);
                case Type.OBJECT -> addInternal(type.getInternalName(), kind, locationKind,
                        sourceMember, sourceDescriptor, targetMember, targetDescriptor, lineNumber);
                case Type.METHOD -> {
                    for (Type argument : type.getArgumentTypes()) {
                        addType(argument, kind, locationKind, sourceMember, sourceDescriptor,
                                targetMember, targetDescriptor, lineNumber);
                    }
                    addType(type.getReturnType(), kind, locationKind, sourceMember,
                            sourceDescriptor, targetMember, targetDescriptor, lineNumber);
                }
                default -> {
                }
            }
        }

        private void addInternal(String internalName, DependencyKind kind, LocationKind locationKind,
                String sourceMember, String sourceDescriptor, String targetMember,
                Integer lineNumber) {
            addInternal(internalName, kind, locationKind, sourceMember, sourceDescriptor,
                    targetMember, null, lineNumber);
        }

        private void addInternal(String internalName, DependencyKind kind, LocationKind locationKind,
                String sourceMember, String sourceDescriptor, String targetMember,
                String targetDescriptor, Integer lineNumber) {
            if (internalName == null) {
                return;
            }
            String targetClass = dotted(internalName);
            if (targetClass.equals(className)) {
                return;
            }
            dependencies.add(new ClassDependency(
                    className,
                    sourceMember,
                    sourceDescriptor,
                    targetClass,
                    targetMember,
                    targetDescriptor,
                    kind,
                    locationKind,
                    lineNumber
            ));
        }

        private void addHandle(Handle handle, DependencyKind kind, String sourceMember,
                String sourceDescriptor, Integer lineNumber) {
            addInternal(handle.getOwner(), kind, LocationKind.INSTRUCTION, sourceMember,
                    sourceDescriptor, handle.getName(), handle.getDesc(), lineNumber);
            Type handleType = handle.getTag() <= Opcodes.H_PUTSTATIC
                    ? Type.getType(handle.getDesc())
                    : Type.getMethodType(handle.getDesc());
            addType(handleType, kind, LocationKind.INSTRUCTION, sourceMember,
                    sourceDescriptor, handle.getName(), handle.getDesc(), lineNumber);
        }

        private void addConstant(Object value, DependencyKind kind, LocationKind locationKind,
                String sourceMember, String sourceDescriptor, Integer lineNumber) {
            if (value instanceof Type type) {
                addType(type, kind, locationKind, sourceMember, sourceDescriptor,
                        null, null, lineNumber);
            } else if (value instanceof Handle handle) {
                addHandle(handle, kind, sourceMember, sourceDescriptor, lineNumber);
            } else if (value instanceof ConstantDynamic constantDynamic) {
                addType(Type.getType(constantDynamic.getDescriptor()), DependencyKind.CONSTANT_DYNAMIC,
                        locationKind, sourceMember, sourceDescriptor, constantDynamic.getName(),
                        constantDynamic.getDescriptor(), lineNumber);
                addHandle(constantDynamic.getBootstrapMethod(), DependencyKind.METHOD_HANDLE,
                        sourceMember, sourceDescriptor, lineNumber);
                for (int index = 0; index < constantDynamic.getBootstrapMethodArgumentCount(); index++) {
                    addConstant(constantDynamic.getBootstrapMethodArgument(index),
                            DependencyKind.CONSTANT_DYNAMIC, locationKind,
                            sourceMember, sourceDescriptor, lineNumber);
                }
            }
        }

        private void readConstantPool(ClassReader reader) {
            char[] buffer = new char[reader.getMaxStringLength()];
            for (int index = 1; index < reader.getItemCount(); index++) {
                try {
                    addConstant(reader.readConst(index, buffer), DependencyKind.CONSTANT_POOL,
                            LocationKind.CLASS, null, null, null);
                } catch (IllegalArgumentException ignored) {
                    // Non-loadable constant-pool entries are exposed through the normal visitors.
                }
            }
        }

        private ClassMetadata toMetadata(String module) {
            return new ClassMetadata(module, className, superName, interfaces, annotations,
                    interfaceType, new ArrayList<>(dependencies));
        }

        private final class DependencyMethodVisitor extends MethodVisitor {

            private final String methodName;
            private final String methodDescriptor;
            private Integer lineNumber;

            private DependencyMethodVisitor(String methodName, String methodDescriptor) {
                super(Opcodes.ASM9);
                this.methodName = methodName;
                this.methodDescriptor = methodDescriptor;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                addType(Type.getType(descriptor), DependencyKind.ANNOTATION, LocationKind.METHOD,
                        methodName, methodDescriptor, null, null, lineNumber);
                return annotationVisitor(methodName, methodDescriptor, LocationKind.METHOD, lineNumber);
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor,
                    boolean visible) {
                addType(Type.getType(descriptor), DependencyKind.ANNOTATION, LocationKind.METHOD,
                        methodName, methodDescriptor, null, null, lineNumber);
                return annotationVisitor(methodName, methodDescriptor, LocationKind.METHOD, lineNumber);
            }

            @Override
            public void visitLineNumber(int line, Label start) {
                lineNumber = line;
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                DependencyKind kind = switch (opcode) {
                    case Opcodes.NEW -> DependencyKind.NEW;
                    case Opcodes.ANEWARRAY -> DependencyKind.ARRAY;
                    case Opcodes.CHECKCAST -> DependencyKind.CAST;
                    case Opcodes.INSTANCEOF -> DependencyKind.INSTANCEOF;
                    default -> DependencyKind.CONSTANT_POOL;
                };
                addInternal(type, kind, LocationKind.INSTRUCTION,
                        methodName, methodDescriptor, null, lineNumber);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                DependencyKind kind = opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC
                        ? DependencyKind.FIELD_READ : DependencyKind.FIELD_WRITE;
                addInternal(owner, kind, LocationKind.INSTRUCTION, methodName,
                        methodDescriptor, name, descriptor, lineNumber);
                addType(Type.getType(descriptor), kind, LocationKind.INSTRUCTION,
                        methodName, methodDescriptor, name, descriptor, lineNumber);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name,
                    String descriptor, boolean isInterface) {
                DependencyKind kind = "<init>".equals(name)
                        ? DependencyKind.CONSTRUCTOR_CALL : DependencyKind.METHOD_CALL;
                addInternal(owner, kind, LocationKind.INSTRUCTION, methodName,
                        methodDescriptor, name, descriptor, lineNumber);
                addType(Type.getMethodType(descriptor), kind, LocationKind.INSTRUCTION,
                        methodName, methodDescriptor, name, descriptor, lineNumber);
            }

            @Override
            public void visitInvokeDynamicInsn(String name, String descriptor,
                    Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                addType(Type.getMethodType(descriptor), DependencyKind.INVOKEDYNAMIC,
                        LocationKind.INSTRUCTION, methodName, methodDescriptor,
                        name, descriptor, lineNumber);
                addHandle(bootstrapMethodHandle, DependencyKind.METHOD_HANDLE,
                        methodName, methodDescriptor, lineNumber);
                for (Object argument : bootstrapMethodArguments) {
                    if (argument instanceof Handle handle) {
                        addHandle(handle, DependencyKind.LAMBDA_TARGET,
                                methodName, methodDescriptor, lineNumber);
                    } else {
                        addConstant(argument, DependencyKind.INVOKEDYNAMIC,
                                LocationKind.INSTRUCTION, methodName, methodDescriptor, lineNumber);
                    }
                }
            }

            @Override
            public void visitLdcInsn(Object value) {
                addConstant(value, DependencyKind.CONSTANT_POOL, LocationKind.INSTRUCTION,
                        methodName, methodDescriptor, lineNumber);
            }

            @Override
            public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
                addType(Type.getType(descriptor), DependencyKind.ARRAY, LocationKind.INSTRUCTION,
                        methodName, methodDescriptor, null, descriptor, lineNumber);
            }

            @Override
            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                addInternal(type, DependencyKind.THROWS, LocationKind.INSTRUCTION,
                        methodName, methodDescriptor, null, lineNumber);
            }

            @Override
            public void visitLocalVariable(String name, String descriptor, String signature,
                    Label start, Label end, int index) {
                addType(Type.getType(descriptor), DependencyKind.SIGNATURE, LocationKind.METHOD,
                        methodName, methodDescriptor, null, null, lineNumber);
                addSignature(signature, methodName, methodDescriptor, LocationKind.METHOD, lineNumber);
            }
        }
    }

    private static String dotted(String internalName) {
        return internalName == null ? null : internalName.replace('/', '.');
    }
}
