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
            //buf1.append("null");
            return;
        }
        if (raw1) {
            if(type == CLASS_SIGNATURE || type == FIELD_SIGNATURE || type == METHOD_SIGNATURE) {
                if(type != CLASS_SIGNATURE) {
                    buf1.append(tab);
                }
                buf1.append( "// signature ").append( desc ).append('\n');
            } else {
                buf1.append(desc);
            }
        } else {
            switch (type) {
                case INTERNAL_NAME :
                    buf1.append(eatPackageNames(desc, '/'));
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
                    buf1.append(") : ");
                    buf1.append(getSimpleName(res));
                    break;

                case METHOD_SIGNATURE :
                case FIELD_SIGNATURE :
                case CLASS_SIGNATURE :
                    // ignore - show only in "raw" mode
                    break;
                case TYPE_DECLARATION :
                    buf1.append(eatPackageNames(desc, '.'));
                    break;
                case CLASS_DECLARATION :
                    buf1.append(eatPackageNames(desc, '.'));
                    break;
                case PARAMETERS_DECLARATION :
                    buf1.append(eatPackageNames(desc, '.'));
                    break;
                default :
                    buf1.append(desc);
            }
        }
    }

    /**
     *
     * @param t
     * @return simply class name without any package/outer class information
     */
    private static String getSimpleName(Type t) {
        String name = t.getClassName();
        return eatPackageNames(name, '.');
    }

    /**
     *
     * @param name Java type name(s).
     * @return simply class name(s) without any package/outer class information,
     * but with "generics" information from given name parameter.
     */
    private  static String eatPackageNames(String name, char separator){
        int lastPoint = name.lastIndexOf(separator);
        if(lastPoint < 0){
            return name;
        }
        StringBuffer sb = new StringBuffer(name);
        do {
            int start = getPackageStartIndex(sb, separator, lastPoint);
            sb.delete(start, lastPoint + 1);
            lastPoint = lastIndexOf(sb, separator, start);
        } while (lastPoint > 0);

        return sb.toString();
    }
    private static int lastIndexOf(StringBuffer chars, char c, int lastPoint){
        for (int i = lastPoint - 1; i > 0; i--) {
            if(chars.charAt(i) == c){
                return i;
            }
        }
        return -1;
    }

    private static int getPackageStartIndex(StringBuffer chars, char c, int firstPoint){
        for (int i = firstPoint - 1; i >= 0; i--) {
            char curr = chars.charAt(i);
            if(curr != c && !Character.isJavaIdentifierPart(curr)){
                return i + 1;
            }
        }
        return 0;
    }

    class CommentedAnnotationVisitor extends TraceAnnotationVisitor {

        protected TraceAnnotationVisitor createTraceAnnotationVisitor() {
            return new CommentedAnnotationVisitor();
        }

        protected void appendDescriptor(final int type, final String desc) {
            CommentedClassVisitor.this.appendDescriptor(buf, type, desc, raw);
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

        private Index getIndex(Label label){
            Index index;
            for (int i = 0; i < text.size(); i++) {
                Object o = text.get(i);
                if(o  instanceof Index){
                    index = (Index)o;
                    if(index.label == label){
                        return index;
                    }
                }
            }
            return null;
        }

        public void visitMethodInsn (
            final int opcode,
            final String owner,
            final String name,
            final String desc)
          {
            buf.setLength(0);
            buf.append(tab2).append(OPCODES[opcode]).append(' ');
            appendDescriptor(INTERNAL_NAME, owner);
            buf.append('.').append(name);
            appendDescriptor(METHOD_DESCRIPTOR, desc);
            buf.append('\n');
            text.add(buf.toString());
          }

        public void visitVarInsn(final int opcode, final int var) {
            text.add(tab2 + OPCODES[opcode] + " " + var);
            if (!raw) {
                text.add(new Integer(var));
            }
            text.add("\n");
        }

        public void visitLabel(Label label) {
            buf.setLength(0);
            buf.append(ltab);
            appendLabel(label);
            Index index = getIndex(label);
            if(index != null){
                buf.append(" (").append(index.insn).append(")");
            }
            buf.append('\n');
            text.add(buf.toString());
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
