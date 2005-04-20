package de.loskutov.bco.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.tree.analysis.Value;

import de.loskutov.bco.BytecodeOutlinePlugin;

/**
 * @author Eric Bruneton
 */

public class DecompiledMethod {

    private List text;

    private List localVariables;

    private Map sourceLines; // decompiled line -> source line

    private Map decompiledLines; // source line -> decompiled line

    private Map insns; // decompiled line -> insn

    private Map opcodes; // decompiled line -> opcode

    private Map insnLines; // insn -> decompile line

    private int lineCount;

    private MethodNode meth;

    private Frame[] frames;

    private String error;

    private int errorInsn;

    public DecompiledMethod(final String owner, final List inputText,
        final Map lineNumbers, final List localVariables,
        final MethodNode meth, final ClassLoader cl) {
        this.text = new ArrayList();
        this.localVariables = localVariables;
        this.sourceLines = new HashMap();
        this.decompiledLines = new HashMap();
        this.insns = new HashMap();
        this.opcodes = new HashMap();
        this.insnLines = new HashMap();

        this.meth = meth;

        formatText(inputText, new HashMap(), new StringBuffer(), this.text);
        computeMaps(lineNumbers);

        if (meth != null &&
            (meth.access & Opcodes.ACC_ABSTRACT)==0) {
            analyzeMethod(owner, cl);
        } else {
//            System.out.println("\nabstr:" + (meth.access & Opcodes.ACC_ABSTRACT));
        }
    }

    /**
     * @param owner
     * @param meth
     * @param cl
     */
    private void analyzeMethod(final String owner, final ClassLoader cl) {
        Analyzer a = new Analyzer(new SimpleVerifier() {

            protected Class getClass(final Type t) {
                try {
                    if (t.getSort() == Type.ARRAY) {
                        return cl.loadClass(t.getDescriptor().replace(
                            '/', '.'));
                    }
                    return cl.loadClass(t.getClassName());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }
        });
        try {
            a.analyze(owner, meth);
        } catch (AnalyzerException e) {
            error = e.getMessage();
            if (error.startsWith("Error at instruction ")) {
                error = error.substring("Error at instruction ".length());
                errorInsn = Integer.parseInt(error.substring(0, error
                    .indexOf(':')));
                error = error.substring(error.indexOf(':') + 2);
            } else {
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
                error = null;
            }
        }
        frames = a.getFrames();
    }

    private void formatText(final List input, final Map locals, StringBuffer line,
        final List result) {
        for (int i = 0; i < input.size(); ++i) {
            Object o = input.get(i);
            if (o instanceof List) {
                formatText((List) o, locals, line, result);
            } else if (o instanceof Index) {
                result.add(o);
                updateLocals((Index) o, locals);
            } else if (o instanceof Integer) {
                String localVariableName = (String) locals.get(o);
                if (localVariableName == null) {
                    Index index = getNextIndex(input, i);
                    if(index != null){
                        updateLocals(index, locals);
                        localVariableName = (String) locals.get(o);
                    }
                }
                if(localVariableName != null) {
                    line.append(": ").append(localVariableName);
                }
            } else {
                String s = o.toString();
                int p;
                do {
                    p = s.indexOf('\n');
                    if (p == -1) {
                        line.append(s);
                    } else {
                        result.add(line.toString() + s.substring(0, p + 1));
                        s = s.substring(p + 1);
                        line.setLength(0);
                    }
                } while (p != -1);
            }
        }
    }

    /**
     * @param i
     * @param input
     * @return
     */
    private Index getNextIndex(List input, int startOffset) {
        for (int i = startOffset + 1; i < input.size(); i++) {
            Object object = input.get(i);
            if(object instanceof Index){
                return (Index)object;
            }
        }
        return null;
    }

    private void updateLocals(final Index index, final Map locals) {
        for (int i = 0; i < localVariables.size(); ++i) {
            LocalVariableNode lvNode = (LocalVariableNode) localVariables.get(i);
            if (lvNode.start == index.label) {
                locals.put(new Integer(lvNode.index), lvNode.name);
            } else if (lvNode.end == index.label) {
                locals.remove(new Integer(lvNode.index));
            }
        }
    }

    private void computeMaps(final Map lineNumbers) {
        int currentSourceLine = -1;
        int currentDecompiledLine = 0;
        int currentInsn = -1;
        int currentOpcode = -1;
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof Index) {
                Index index = (Index) o;
                Integer sourceLine = (Integer) lineNumbers.get(index.label);
                if (sourceLine != null) {
                    currentSourceLine = sourceLine.intValue();
                }
                currentInsn = index.insn;
                currentOpcode = index.opcode;
            } else {
                ++currentDecompiledLine;
            }
            Integer csl = new Integer(currentSourceLine);
            Integer cdl = new Integer(currentDecompiledLine);
            Integer ci = new Integer(currentInsn);
            Integer co = new Integer(currentOpcode);
            sourceLines.put(cdl, csl);
            if (decompiledLines.get(csl) == null) {
                decompiledLines.put(csl, cdl);
            }
            insns.put(cdl, ci);
            opcodes.put(cdl, co);
            if (insnLines.get(ci) == null) {
                insnLines.put(ci, cdl);
            }
        }
        lineCount = currentDecompiledLine;
    }

    public String getText() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (!(o instanceof Index)) {
                buf.append((String) o);
            }
        }
        return buf.toString();
    }

    public String[][] getTextTable() {
        Frame frame = null;
        List lines = new ArrayList();
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof Index) {
                if (frames != null) {
                    frame = frames[((Index) o).insn];
                }
            } else {
                String locals = " ";
                String stack = " ";
                if (frame != null) {
                    StringBuffer buf = new StringBuffer();
                    appendFrame(buf, frame);
                    int p = buf.indexOf(" ");
                    locals = buf.substring(0, p);
                    if("".equals(locals)){
                        locals = " ";
                    }
                    stack = buf.substring(p + 1);
                    if("".equals(stack)){
                        stack = " ";
                    }
                }
                lines.add(new String[]{locals, stack, o.toString()});
                frame = null;
            }
        }
        return (String[][]) lines.toArray(new String[lines.size()][]);
    }

    public int getLineCount() {
        return lineCount;
    }

    public String getError() {
        return error;
    }

    public int getErrorLine() {
        if (error == null) {
            return -1;
        }
        Integer i = (Integer) insnLines.get(new Integer(errorInsn));
        return i == null
            ? -1
            : i.intValue();
    }

    private void appendFrame(final StringBuffer buf, final Frame f) {
        try {
            for (int i = 0; i < f.getLocals(); ++i) {
                appendValue(buf, f.getLocal(i));
            }
            buf.append(' ');
            for (int i = 0; i < f.getStackSize(); ++i) {
                appendValue(buf, f.getStack(i));
            }
        } catch (IndexOutOfBoundsException e) {
            // TODO should we keep this?
            BytecodeOutlinePlugin.log(e, IStatus.WARNING);
        }
    }

    private void appendValue(final StringBuffer buf, final Value v) {
        if (((BasicValue) v).isReference()) {
            buf.append("R");
        } else {
            buf.append(v.toString());
        }
    }

    public int getSourceLine(final int decompiledLine) {
        Integer i = (Integer) sourceLines.get(new Integer(decompiledLine));
        return i == null
            ? -1
            : i.intValue();
    }

    /**
     *
     * @param decompiledLine
     * @return array with two elements, first is the local variables table,
     * second is the operands stack content. "null" value could be returned too.
     */
    public String[] getFrame(final int decompiledLine, final boolean useQualifiedNames) {
        Integer insn = getBytecodeOffset(decompiledLine);
        if (error != null && insn != null && insn.intValue() == errorInsn) {
            return new String [] {error,error};
        }
        if (frames != null && insn != null) {
            Frame f = frames[insn.intValue()];
            if (f == null) {
                return null;
            }

            try {
                StringBuffer localsBuf = new StringBuffer();

                for (int i = 0; i < f.getLocals(); ++i) {
                    String s = f.getLocal(i).toString();
                    appendTypeName(i, useQualifiedNames, localsBuf, s);

                    for (Iterator it = localVariables.iterator(); it.hasNext();) {
                        LocalVariableNode lvnode = (LocalVariableNode) it.next();
                        int n = lvnode.index;
                        if( n==i) {
                          localsBuf.append( " : ").append( lvnode.name);
                        }
                    }

                    localsBuf.append('\n');
                }
                StringBuffer stackBuf = new StringBuffer();
                for (int i = 0; i < f.getStackSize(); ++i) {
                    String s = f.getStack(i).toString();
                    appendTypeName(i, useQualifiedNames, stackBuf, s);
                    stackBuf.append('\n');
                }
                return new String[] {localsBuf.toString(), stackBuf.toString()};
            } catch (IndexOutOfBoundsException e) {
                // TODO should we keep this?
                BytecodeOutlinePlugin.log(e, IStatus.WARNING);
            }
        }
        return null;
    }

    /**
     * @param decompiledLine
     * @return
     */
    public Integer getBytecodeOffset(final int decompiledLine) {
        Integer insn = (Integer) insns.get(new Integer(decompiledLine));
        return insn;
    }

    /**
     * @param decompiledLine
     * @return
     */
    public Integer getBytecodeInsn(final int decompiledLine) {
        Integer insn = (Integer) opcodes.get(new Integer(decompiledLine));
        return insn;
    }

    public String[][][] getFrameTables(final int decompiledLine, boolean useQualifiedNames) {
      Integer insn = getBytecodeOffset(decompiledLine);
      if (error != null && insn != null && insn.intValue() == errorInsn) {
          return null;
      }
      if (frames != null && insn != null) {
        Frame f = frames[insn.intValue()];
        if (f == null) {
            return null;
        }

        try {
            ArrayList locals = new ArrayList();
            for (int i = 0; i < f.getLocals(); ++i) {
                String varName = "";
                for (Iterator it = localVariables.iterator(); it.hasNext();) {
                    LocalVariableNode lvnode = (LocalVariableNode) it.next();
                    int n = lvnode.index;
                    if( n==i) {
                      varName = lvnode.name;
                      // TODO take into account variable scope!
                      break;
                    }
                }

                locals.add( new String[] {
                    ""+i,
                    getTypeName( useQualifiedNames, f.getLocal(i).toString()),
                    varName});
            }

            ArrayList stack = new ArrayList();
            for (int i = 0; i < f.getStackSize(); ++i) {
                stack.add( new String[] {
                    ""+i,
                    getTypeName( useQualifiedNames, f.getStack(i).toString())});
            }
            return new String[][][] {
                (String[][]) locals.toArray( new String[ 3][]),
                (String[][]) stack.toArray( new String[ 2][])};
        } catch (IndexOutOfBoundsException e) {
            // TODO should we keep this?
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
      }
      return null;
    }


    /**
     * Appends full type name or only simply name, depends on boolean flag.
     *
     * @param useQualifiedNames if false, then e.g. "Object" will be appended to
     * buffer instead of "Ljava/lang/Object;" etc
     * @param buf buffer to append
     * @param s string with bytecode type name, like "Ljava/lang/Object;"
     */
    private void appendTypeName(int n, final boolean useQualifiedNames, StringBuffer buf, String s) {
        buf.append(n).append( " ");
        if(!useQualifiedNames) {
            int idx = s.lastIndexOf('/');
            if(idx > 0){
                // from "Ljava/lang/Object;" to "Object"
                buf.append(s.substring(idx + 1, s.length() - 1));
                return;
            }
        }
        if("Lnull;".equals(s)){
            buf.append("null");
        } else {
            buf.append(s);
        }
    }

    private String getTypeName(final boolean useQualifiedNames, String s) {
        if (!useQualifiedNames) {
            int idx = s.lastIndexOf('/');
            if (idx > 0) {
                // from "Ljava/lang/Object;" to "Object"
                return s.substring(idx + 1, s.length() - 1);
            }
            // this is the case on LVT view - ignore it
            if("." == s){
                return s;
            }
            // resolve primitive types
            return CommentedClassVisitor.getSimpleName(Type.getType(s));
        }
        return "Lnull;".equals(s) ? "null" : s;
    }

    public int getDecompiledLine(final int sourceLine) {
        Integer i = (Integer) decompiledLines.get(new Integer(sourceLine));
        return i == null
            ? -1
            : i.intValue();
    }
}
