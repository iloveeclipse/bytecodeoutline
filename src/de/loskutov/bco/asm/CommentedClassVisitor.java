package de.loskutov.bco.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.TraceAnnotationVisitor;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceFieldVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 * @author Eric Bruneton
 */

public class CommentedClassVisitor extends TraceClassVisitor {

    protected boolean raw;

    public CommentedClassVisitor(final boolean raw) {
        super(null);
        this.raw = raw;
    }

    public void visitEnd() {
        text.add("}\n");
    }

    protected TraceAnnotationVisitor createTraceAnnotationVisitor() {
        return new CommentedAnnotationVisitor();
    }

    protected TraceFieldVisitor createTraceFieldVisitor() {
        return new CommentedFieldVisitor();
    }

    protected TraceMethodVisitor createTraceMethodVisitor() {
        return new CommentedMethodVisitor();
    }

    protected void appendDescriptor(final int type, final String desc) {
        appendDescriptor(buf, type, desc, raw);
    }

    protected void appendDescriptor(final StringBuffer buf1, final int type,
        final String desc, final boolean raw1) {
        if (desc == null) {
            buf1.append("null");
            return;
        }
        if (raw1) {
            buf1.append(desc);
        } else {
            switch (type) {
                case INTERNAL_NAME :
                    int p = desc.lastIndexOf('/');
                    if (p == -1) {
                        buf1.append(desc);
                    } else {
                        buf1.append(desc.substring(p + 1));
                    }
                    break;
                case FIELD_DESCRIPTOR :
                    buf1.append(getSimpleName(Type.getType(desc)));
                    break;
                case METHOD_DESCRIPTOR :
                    Type[] args = Type.getArgumentTypes(desc);
                    Type res = Type.getReturnType(desc);
                    buf1.append('(');
                    for (int i = 0; i < args.length; ++i) {
                        if (i > 0) {
                            buf1.append(',');
                        }
                        buf1.append(getSimpleName(args[i]));
                    }
                    buf1.append(')');
                    buf1.append(getSimpleName(res));
                    break;
                default :
                    buf1.append(desc);
            }
        }
    }

    private String getSimpleName(Type t) {
        String name = t.getClassName();
        int p = name.lastIndexOf('.');
        if (p == -1) {
            return name;
        }
        return name.substring(p + 1);
    }

    class CommentedAnnotationVisitor extends TraceAnnotationVisitor {

        protected void appendDescriptor(final String desc) {
            CommentedClassVisitor.this.appendDescriptor(
                buf, FIELD_DESCRIPTOR, desc, raw);
        }

        protected TraceAnnotationVisitor createTraceAnnotationVisitor() {
            return new CommentedAnnotationVisitor();
        }
    }

    class CommentedFieldVisitor extends TraceFieldVisitor {

        protected void appendDescriptor(final int type, final String desc) {
            CommentedClassVisitor.this.appendDescriptor(buf, type, desc, raw);
        }

        protected TraceAnnotationVisitor createTraceAnnotationVisitor() {
            return new CommentedAnnotationVisitor();
        }
    }

    class CommentedMethodVisitor extends TraceMethodVisitor {

        public void visitVarInsn(final int opcode, final int var) {
            text.add(tab2 + OPCODES[opcode] + " " + var);
            if (!raw) {
                text.add(new Integer(var));
            }
            text.add("\n");
        }

        public void visitIincInsn(final int var, final int increment) {
            text.add(tab2 + "IINC " + var);
            if (!raw) {
                text.add(new Integer(var));
            }
            text.add(" " + increment + "\n");
        }

        public void visitLocalVariable(final String name, final String desc,
            final String signature, final Label start, final Label end,
            final int index) {
            if (raw) {
                super.visitLocalVariable(
                    name, desc, signature, start, end, index);
            }
        }

        public void visitLineNumber(final int line, final Label start) {
            if (raw) {
                super.visitLineNumber(line, start);
            }
        }

        public void visitMaxs(final int maxStack, final int maxLocals) {
            if (raw) {
                super.visitMaxs(maxStack, maxLocals);
            }
        }

        protected TraceAnnotationVisitor createTraceAnnotationVisitor() {
            return new CommentedAnnotationVisitor();
        }

        protected void appendDescriptor(final int type, final String desc) {
            CommentedClassVisitor.this.appendDescriptor(buf, type, desc, raw);
        }
    }
}
