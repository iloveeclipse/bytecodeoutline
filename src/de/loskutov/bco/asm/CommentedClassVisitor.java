package de.loskutov.bco.asm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.BitSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.AbstractVisitor;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceVisitor;

import de.loskutov.bco.preferences.BCOConstants;

/**
 * @author Eric Bruneton
 */

public class CommentedClassVisitor extends TraceVisitor implements ICommentedClassVisitor {

    protected boolean raw;
    protected BitSet modes;
    protected boolean showLines;
    protected boolean showLocals;
    protected boolean showStackMap;
    protected boolean showHex;
    private TraceClassVisitor traceClassVisitor;

    public CommentedClassVisitor(final BitSet modes) {
        super(Opcodes.ASM4);
        this.modes = modes;
        raw = !modes.get(BCOConstants.F_SHOW_RAW_BYTECODE);
        showLines = modes.get(BCOConstants.F_SHOW_LINE_INFO);
        showLocals = modes.get(BCOConstants.F_SHOW_VARIABLES);
        showStackMap = modes.get(BCOConstants.F_SHOW_STACKMAP);
        showHex = modes.get(BCOConstants.F_SHOW_HEX_VALUES);
    }

    protected void appendDescriptor(final int type, final String desc) {
        appendDescriptor(buf, type, desc, raw);
    }

    protected void appendDescriptor(final StringBuffer buf1, final int type,
        final String desc, final boolean raw1) {
        if (desc == null) {
            return;
        }
        if (raw1) {
            if (type == CLASS_SIGNATURE || type == FIELD_SIGNATURE
                || type == METHOD_SIGNATURE) {
                buf1.append("// signature ").append(desc).append('\n');
            } else {
                buf1.append(desc);
            }
        } else {
            switch (type) {
                case INTERNAL_NAME :
                    buf1.append(eatPackageNames(desc, '/'));
                    break;
                case FIELD_DESCRIPTOR :
                    if ("T".equals(desc)) {
                        buf1.append("top");
                    } else if ("N".equals(desc)) {
                        buf1.append("null");
                    } else if ("U".equals(desc)) {
                        buf1.append("uninitialized_this");
                    } else {
                        buf1.append(getSimpleName(Type.getType(desc)));
                    }
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
                    // fine tuning of identation - we have two tabs in this case
                    if (buf.lastIndexOf(tab) == buf.length() - tab.length()) {
                        buf.delete(buf.lastIndexOf(tab), buf.length());
                    }
                    break;

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
     * @param t
     * @return simply class name without any package/outer class information
     */
    public static String getSimpleName(Type t) {
        String name = t.getClassName();
        return eatPackageNames(name, '.');
    }

    /**
     * @param name Java type name(s).
     * @return simply class name(s) without any package/outer class information, but with
     * "generics" information from given name parameter.
     */
    private static String eatPackageNames(String name, char separator) {
        int lastPoint = name.lastIndexOf(separator);
        if (lastPoint < 0) {
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

    private static int lastIndexOf(StringBuffer chars, char c, int lastPoint) {
        for (int i = lastPoint - 1; i > 0; i--) {
            if (chars.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }

    private static int getPackageStartIndex(StringBuffer chars, char c,
        int firstPoint) {
        for (int i = firstPoint - 1; i >= 0; i--) {
            char curr = chars.charAt(i);
            if (curr != c && !Character.isJavaIdentifierPart(curr)) {
                return i + 1;
            }
        }
        return 0;
    }


    /**
     * control chars names
     */
    private static final String[] CHAR_NAMES = {"NUL", "SOH", "STX", "ETX",
        "EOT", "ENQ", "ACK", "BEL", "BS", "HT", "LF", "VT", "FF", "CR", "SO",
        "SI", "DLE", "DC1", "DC2", "DC3", "DC4", "NAK", "SYN", "ETB", "CAN",
        "EM", "SUB", "ESC", "FS", "GS", "RS", "US", // "Sp"
    };

    private Index getIndex(Label label) {
        Index index;
        for (int i = 0; i < text.size(); i++) {
            Object o = text.get(i);
            if (o instanceof Index) {
                index = (Index) o;
                if (index.labelNode != null
                    && index.labelNode.getLabel() == label) {
                    return index;
                }
            }
        }
        return null;
    }

    public void visitFrame(int type, int nLocal, Object[] local,
        int nStack, Object[] stack) {
        if (showStackMap) {
            super.visitFrame(type, nLocal, local, nStack, stack);
        }
    }

    public void visitMethodInsn(final int opcode, final String owner,
        final String name, final String desc) {
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
        if (index != null) {
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

    public void visitIntInsn(int opcode, int operand) {
        buf.setLength(0);
        buf.append(tab2).append(OPCODES[opcode]).append(' ').append(
            opcode == Opcodes.NEWARRAY
            ? TYPES[operand]
                : formatValue(operand)).append('\n');
        text.add(buf.toString());

        // TODO ASM 4.0 transition:  seems to be dead code now
        //            if (mv != null) {
        //                mv.visitIntInsn(opcode, operand);
        //            }
    }

    private String formatValue(int operand) {
        if (showHex) {
            String intStr = Integer.toHexString(operand).toUpperCase();
            return intStr + getAsCharComment(operand);
        }
        return Integer.toString(operand);
    }

    /**
     * @param value
     * @return char value from int, together with char name if it is a control char,
     * or an empty string
     */
    private String getAsCharComment(int value) {
        if (Character.MAX_VALUE < value || Character.MIN_VALUE > value) {
            return "";
        }
        StringBuffer sb = new StringBuffer("    // '");
        switch (value) {
            case '\t' :
                sb.append("\\t");
                break;
            case '\r' :
                sb.append("\\r");
                break;
            case '\n' :
                sb.append("\\n");
                break;
            case '\f' :
                sb.append("\\f");
                break;
            default :
                sb.append((char) value);
                break;
        }

        if (value >= CHAR_NAMES.length) {
            if (value == 127) {
                return sb.append("' (DEL)").toString();
            }
            return sb.append("'").toString();
        }
        return sb.append("' (").append(CHAR_NAMES[value]).append(")")
            .toString();
    }

    private String formatValue(Object operand) {
        if (operand == null) {
            return "null";
        }
        if (showHex) {
            if (operand instanceof Integer) {
                String intStr = Integer.toHexString(
                    ((Integer) operand).intValue()).toUpperCase();
                return intStr
                    + getAsCharComment(((Integer) operand).intValue());
            } else if (operand instanceof Long) {
                return Long.toHexString(((Long) operand).longValue())
                    .toUpperCase();
            } else if (operand instanceof Double) {
                return Double.toHexString(((Double) operand).doubleValue());
            } else if (operand instanceof Float) {
                return Float.toHexString(((Float) operand).floatValue());
            }
        }
        return operand.toString();
    }

    public void visitLocalVariable(final String name, final String desc,
        final String signature, final Label start, final Label end,
        final int index) {
        if (showLocals) {
            super.visitLocalVariable(
                name, desc, signature, start, end, index);
        }
    }

    public void visitLdcInsn(Object cst) {
        buf.setLength(0);
        buf.append(tab2).append("LDC ");
        if (cst instanceof String) {
            AbstractVisitor.appendString(buf, (String) cst);
        } else if (cst instanceof Type) {
            buf.append(((Type) cst).getDescriptor() + ".class");
        } else {
            buf.append(formatValue(cst));
        }
        buf.append('\n');
        text.add(buf.toString());

        // TODO ASM 4.0 transition: seems to be dead code now
        //            if (mv != null) {
        //                mv.visitLdcInsn(cst);
        //            }
    }

    public void visitLineNumber(final int line, final Label start) {
        if (showLines) {
            super.visitLineNumber(line, start);
        }
    }

    public void visitMaxs(final int maxStack, final int maxLocals) {
        if (showLocals) {
            super.visitMaxs(maxStack, maxLocals);
        }
    }

    public ClassVisitor getClassVisitor() {
        if(traceClassVisitor == null) {
            // TODO ASM 4.0 transition: PrintWriter should be optional
            traceClassVisitor = new TraceClassVisitor(null, this, new PrintWriter(new StringWriter()));
        }
        return traceClassVisitor;
    }

    protected TraceVisitor createTraceVisitor() {
        return new CommentedClassVisitor(modes);
    }
}
