/*******************************************************************************
 * Copyright (c) 2011 Eric Bruneton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Eric Bruneton - initial API and implementation
 * Contributor:  Andrey Loskutov - fixes
 *******************************************************************************/
package de.loskutov.bco.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.objectweb.asm.Label;
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
import de.loskutov.bco.preferences.BCOConstants;

/**
 * @author Eric Bruneton
 */

public class DecompiledMethod {

    private final List text;

    private final List localVariables;

    /**
     * decompiled line -> source line
     */
    private final Map sourceLines;

    /**
     * source line -> decompiled line
     */
    private final Map decompiledLines;

    /**
     * decompiled line -> insn
     */
    private final Map insns;

    /**
     *  decompiled line -> opcode
     */
    private final Map opcodes;

    /**
     * insn -> decompile line
     */
    private final Map insnLines;

    private int lineCount;

    /**
     * first source line, if any
     */
    private int firstSourceLine;

    /**
     * last source line, if any
     */
    private int lastSourceLine;

    MethodNode meth;

    private Frame[] frames;

    private String error;

    private int errorInsn;

    private final String owner;


    private final Map lineNumbers;

    private final DecompilerOptions options;

    private final int access;


    public DecompiledMethod(final String owner,
        final Map lineNumbers, final MethodNode meth, DecompilerOptions options, int access) {
        this.meth = meth;
        this.owner = owner;
        this.lineNumbers = lineNumbers;
        this.options = options;
        this.access = access;
        this.text = new ArrayList();
        this.localVariables = meth.localVariables;
        this.sourceLines = new HashMap();
        this.decompiledLines = new HashMap();
        this.insns = new HashMap();
        this.opcodes = new HashMap();
        this.insnLines = new HashMap();
    }

    void setText(final List inputText) {
        formatText(inputText, new HashMap(), new StringBuffer(), this.text);
        computeMaps(lineNumbers);

        if (options.modes.get(BCOConstants.F_SHOW_ANALYZER)
            && (access & Opcodes.ACC_ABSTRACT) == 0) {
            analyzeMethod(options.cl);
        }
    }

    void addLineNumber(Label start, Integer integer) {
        lineNumbers.put(start, integer);
    }

    public boolean isInit() {
        return ("<init>".equals(meth.name) && "()V".equals(meth.desc))
            || "<clinit>".equals(meth.name);
    }

    public boolean hasSourceLinesInfo(){
        return ! sourceLines.isEmpty();
    }

    public boolean hasLocalVariablesInfo(){
        return ! localVariables.isEmpty();
    }

    public String getSignature(){
        return meth.name + meth.desc;
    }

    public boolean containsSource(int sourceLine){
        return sourceLine >= getFirstSourceLine() && sourceLine <= getLastSourceLine();
    }

    /**
     * @param sourceLine
     * @return nearest match above given source line or the given line for perfect match
     * or -1 for no match. The return value is method-relative, and need to be transformed
     * to class absolute
     */
    public int getBestDecompiledLine(final int sourceLine){
        if(!containsSource(sourceLine)){
            return -1;
        }
        Set set = decompiledLines.keySet();
        if(set.size() == 0){
            return -1;
        }
        int bestMatch = -1;
        for (Iterator iter = set.iterator(); iter.hasNext();) {
            int line = ((Integer) iter.next()).intValue();
            int delta = sourceLine - line;
            if(delta < 0){
                continue;
            } else if(delta == 0){
                return line;
            }
            if(bestMatch < 0 || delta < sourceLine - bestMatch){
                bestMatch = line;
            }
        }
        if(bestMatch < 0){
            return -1;
        }
        return ((Integer)decompiledLines.get(new Integer(bestMatch))).intValue();
    }

    /**
     * @param owner
     * @param meth
     * @param cl
     */
    private void analyzeMethod(final ClassLoader cl) {
        Analyzer a = new Analyzer(new SimpleVerifier() {

            @Override
            protected Class getClass(final Type t) {
                try {
                    if (t.getSort() == Type.ARRAY) {
                        return Class.forName(t.getDescriptor().replace(
                            '/', '.'), true, cl);
                    }
                    return cl.loadClass(t.getClassName());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e.toString()+" " +cl, e);
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

    private static Index getNextIndex(List input, int startOffset) {
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
            if (lvNode.start == index.labelNode) {
                locals.put(new Integer(lvNode.index), lvNode.name);
            } else if (lvNode.end == index.labelNode) {
                locals.remove(new Integer(lvNode.index));
            }
        }
    }

    private void computeMaps(final Map lineNumbers1) {
        int currentDecompiledLine = 0;
        int firstLine = -1;
        int lastLine = -1;
        for (int i = 0; i < text.size(); ++i) {
            int currentOpcode = -1;
            int currentInsn1 = -1;
            int currentSourceLine = -1;
            Object o = text.get(i);
            if (o instanceof Index) {
                Index index = (Index) o;
                Integer sourceLine = null;
                if(index.labelNode != null) {
                    sourceLine = (Integer) lineNumbers1.get(index.labelNode.getLabel());
                }
                if (sourceLine != null) {
                    currentSourceLine = sourceLine.intValue();
                    if(firstLine == -1 || currentSourceLine < firstLine){
                        firstLine = currentSourceLine;
                    }
                    if(lastLine == -1 || currentSourceLine > lastLine){
                        lastLine = currentSourceLine;
                    }
                }
                currentInsn1 = index.insn;
                currentOpcode = index.opcode;
            } else {
                ++currentDecompiledLine;
            }
            Integer cdl = new Integer(currentDecompiledLine);
            Integer ci = new Integer(currentInsn1);
            Integer co = new Integer(currentOpcode);
            if(currentSourceLine >= 0){
                Integer csl = new Integer(currentSourceLine);
                sourceLines.put(cdl, csl);
                if (decompiledLines.get(csl) == null) {
                    decompiledLines.put(csl, cdl);
                }
            }
            insns.put(cdl, ci);
            opcodes.put(cdl, co);
            if (insnLines.get(ci) == null) {
                insnLines.put(ci, cdl);
            }
        }
        lineCount = currentDecompiledLine;
        firstSourceLine = firstLine;
        lastSourceLine = lastLine;
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
        String error1 = "";
        List lines = new ArrayList();
        String offsStr = null;
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof Index) {
                Index index = (Index) o;
                int insn = index.insn;

                offsStr = "" + insn;
                if (frames != null && insn < frames.length) {
                    frame = frames[insn];
                    if (this.error != null && insn == this.errorInsn) {
                      error1 = this.error;
                    }
                }
            } else {
                if(offsStr == null){
                    offsStr = "";
                }
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

                lines.add(new String[]{offsStr, locals, stack, o.toString(), error1});
                frame = null;
                error1 = "";
                offsStr = null;
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

    private static void appendFrame(final StringBuffer buf, final Frame f) {
        try {
            for (int i = 0; i < f.getLocals(); ++i) {
                appendValue(buf, f.getLocal(i));
            }
            buf.append(' ');
            for (int i = 0; i < f.getStackSize(); ++i) {
                appendValue(buf, f.getStack(i));
            }
        } catch (IndexOutOfBoundsException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
    }

    private static void appendValue(final StringBuffer buf, final Value v) {
        if (((BasicValue) v).isReference()) {
            buf.append("R");
        } else {
            buf.append(v.toString());
        }
    }

    public int getFirstSourceLine(){
        return firstSourceLine;
    }

    public int getLastSourceLine(){
        return lastSourceLine;
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
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
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
        if(insn == null){
            return null;
        }
        return getFrameTablesForInsn(insn.intValue(), useQualifiedNames);
    }

    public String[][][] getFrameTablesForInsn(final int insn, boolean useQualifiedNames) {
        if (error != null && insn == errorInsn) {
            return null;
        }
        if (frames != null && insn >= 0 && insn < frames.length) {
            Frame f = frames[insn];
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
    private static void appendTypeName(int n, final boolean useQualifiedNames, StringBuffer buf, String s) {
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

    private static String getTypeName(final boolean useQualifiedNames, String s) {
      if (!useQualifiedNames) {
          // get leading array symbols
          String arraySymbols = "";
          while (s.startsWith("[")){
              arraySymbols += "[";
              s = s.substring(1);
          }

          int idx = s.lastIndexOf('/');
          if (idx > 0) {
              // from "Ljava/lang/Object;" to "Object"
              return arraySymbols  + s.substring(idx + 1, s.length() - 1);
          }
          // this is the case on LVT view - ignore it
          if("." == s){
              return arraySymbols  + s;
          }
          // resolve primitive types
          return arraySymbols +
              CommentedClassVisitor.getSimpleName(Type.getType(s));
      }
      return "Lnull;".equals(s) ? "null" : s;
    }

    public int getDecompiledLine(final int sourceLine) {
        Integer i = (Integer) decompiledLines.get(new Integer(sourceLine));
        return i == null
            ? -1
            : i.intValue();
    }

    /**
     * Returns <code>true</code> if this <code>DecompiledMethod</code> is the same as the o argument.
     *
     * @return <code>true</code> if this <code>DecompiledMethod</code> is the same as the o argument.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DecompiledMethod)) {
            return false;
        }
        DecompiledMethod another = (DecompiledMethod) o;
        return getSignature().equals(another.getSignature())
            && (owner != null? owner.equals(another.owner) : true);
    }

    @Override
    public int hashCode() {
        return getSignature().hashCode() + (owner != null? owner.hashCode() : 0);
    }
}
