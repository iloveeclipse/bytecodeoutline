/*******************************************************************************
 * Copyright (c) 2011 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.bco.asm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.BitSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifierClassVisitor;
import org.objectweb.asm.util.ASMifierVisitor;

import de.loskutov.bco.preferences.BCOConstants;

/**
 * @author Andrei
 */
public class CommentedASMifierClassVisitor extends ASMifierVisitor implements ICommentedClassVisitor {

    protected BitSet modes;
    protected boolean raw;
    protected boolean showLines;
    protected boolean showLocals;
    protected boolean showStackMap;
    private ASMifierClassVisitor classVisitor;


    public void visitFrame(final int type, final int nLocal,
        final Object[] local, final int nStack, final Object[] stack) {
        if (showStackMap) {
            super.visitFrame(type, nLocal, local, nStack, stack);
        }
    }

    public void visitLineNumber(int line, Label start) {
        if (showLines) {
            super.visitLineNumber(line, start);
        }
    }

    public void visitLocalVariable(String name1, String desc,
        String signature, Label start, Label end, int index) {
        if (showLocals) {
            super.visitLocalVariable(
                name1, desc, signature, start, end, index);
        }
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        //if (showLocals) {
        super.visitMaxs(maxStack, maxLocals);
        //}
    }

    private CommentedASMifierClassVisitor(final BitSet modes, String name, int id) {
        super(Opcodes.ASM4, name, id);
        this.modes = modes;
        raw = !modes.get(BCOConstants.F_SHOW_RAW_BYTECODE);
        showLines = modes.get(BCOConstants.F_SHOW_LINE_INFO);
        showLocals = modes.get(BCOConstants.F_SHOW_VARIABLES);
        showStackMap = modes.get(BCOConstants.F_SHOW_STACKMAP);
    }

    public CommentedASMifierClassVisitor(final BitSet modes) {
        this(modes, "cw", 0);
    }

    public void visitEnd() {
        text.add("cw.visitEnd();\n\n");
        text.add("return cw.toByteArray();\n");
        text.add("}\n");
        text.add("}\n");
    }

    public ClassVisitor getClassVisitor() {
        if(classVisitor == null) {
            // TODO ASM 4.0 transition: PrintWriter should be optional
            classVisitor = new ASMifierClassVisitor(this, new PrintWriter(new StringWriter()));
        }
        return classVisitor;
    }

    protected ASMifierVisitor createASMifierVisitor(String name1, int id1) {
        return new CommentedASMifierClassVisitor(modes, name1, id1);
    }
}
