package de.loskutov.bco.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Map insnLines; // insn -> decompile line

    private int lineCount;

    private MethodNode meth;

    private Frame[] frames;

    private String error;

    private int errorInsn;

    public DecompiledMethod(final String owner, final List text,
        final Map lineNumbers, final List localVariables,
        final MethodNode meth, final ClassLoader cl) {
        this.text = new ArrayList();
        this.localVariables = localVariables;
        this.sourceLines = new HashMap();
        this.decompiledLines = new HashMap();
        this.insns = new HashMap();
        this.insnLines = new HashMap();
        
        this.meth = meth;

        formatText(text, new HashMap(), "", this.text);
        computeMaps(lineNumbers);

        if (meth != null) {
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
                    BytecodeOutlinePlugin.logError(e);
                    error = null;
                }
            }
            frames = a.getFrames();
        }
    }

    private void formatText(final List input, final Map locals, String line,
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
                if (localVariableName != null) {
                    line = line + ": " + localVariableName;
                }
            } else {
                String s = o.toString();
                int p;
                do {
                    p = s.indexOf('\n');
                    if (p == -1) {
                        line = line + s;
                    } else {
                        result.add(line + s.substring(0, p + 1));
                        s = s.substring(p + 1);
                        line = "";
                    }
                } while (p != -1);
            }
        }
    }

    private void updateLocals(final Index index, final Map locals) {
        for (int i = 0; i < localVariables.size(); ++i) {
            LocalVariableNode lvn = (LocalVariableNode) localVariables.get(i);
            if (lvn.start == index.label) {
                locals.put(new Integer(lvn.index), lvn.name);
            } else if (lvn.end == index.label) {
                locals.remove(new Integer(lvn.index));
            }
        }
    }

    private void computeMaps(final Map lineNumbers) {
        int currentSourceLine = -1;
        int currentDecompiledLine = 0;
        int currentInsn = -1;
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof Index) {
                Index index = (Index) o;
                Integer sourceLine = (Integer) lineNumbers.get(index.label);
                if (sourceLine != null) {
                    currentSourceLine = sourceLine.intValue();
                }
                currentInsn = index.insn;
            } else {
                ++currentDecompiledLine;
            }
            Integer csl = new Integer(currentSourceLine);
            Integer cdl = new Integer(currentDecompiledLine);
            Integer ci = new Integer(currentInsn);
            sourceLines.put(cdl, csl);
            if (decompiledLines.get(csl) == null) {
                decompiledLines.put(csl, cdl);
            }
            insns.put(cdl, ci);
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
                String locals = "";
                String stack = "";
                if (frame != null) {
                    StringBuffer buf = new StringBuffer();
                    appendFrame(buf, frame);
                    int p = buf.indexOf(" ");
                    locals = buf.substring(0, p);
                    stack = buf.substring(p + 1);
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
        } catch (AnalyzerException e) {
            e.printStackTrace();
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

    public String getFrame(final int decompiledLine) {
        Integer insn = (Integer) insns.get(new Integer(decompiledLine));
        if (error != null && insn != null && insn.intValue() == errorInsn) {
            return error;
        }
        if (frames != null && insn != null) {
            Frame f = frames[insn.intValue()];
            if (f == null) {
                return null;
            }

            try {
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < f.getLocals(); ++i) {
                    buf.append(f.getLocal(i)).append('\n');
                }
                buf.append('\n');
                for (int i = 0; i < f.getStackSize(); ++i) {
                    buf.append(f.getStack(i)).append('\n');
                }
                return buf.toString();
            } catch (AnalyzerException e) {
                e.printStackTrace();
            }

        }
        return null;
    }

    public int getDecompiledLine(final int sourceLine) {
        Integer i = (Integer) decompiledLines.get(new Integer(sourceLine));
        return i == null
            ? -1
            : i.intValue();
    }
}
