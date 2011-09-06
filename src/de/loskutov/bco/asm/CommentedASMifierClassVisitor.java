/*******************************************************************************
 * Copyright (c) 2011 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.bco.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.ASMifier;

import de.loskutov.bco.preferences.BCOConstants;

/**
 * @author Andrei
 */
public class CommentedASMifierClassVisitor extends ASMifier implements ICommentedClassVisitor {

    protected final boolean showLines;
    protected final boolean showLocals;
    protected final boolean showStackMap;
    private final DecompilerOptions options;
    private String javaVersion;
    private int accessFlags;

    @Override
    public void visitFrame(final int type, final int nLocal,
        final Object[] local, final int nStack, final Object[] stack) {
        if (showStackMap) {
            super.visitFrame(type, nLocal, local, nStack, stack);
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (showLines) {
            super.visitLineNumber(line, start);
        }
    }

    @Override
    public void visitLocalVariable(String name1, String desc,
        String signature, Label start, Label end, int index) {
        if (showLocals) {
            super.visitLocalVariable(
                name1, desc, signature, start, end, index);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
//        if (showLocals) {
            super.visitMaxs(maxStack, maxLocals);
//        }
    }

    private CommentedASMifierClassVisitor(final DecompilerOptions options, String name, int id) {
        super(Opcodes.ASM4, name, id);
        this.options = options;
        showLines = options.modes.get(BCOConstants.F_SHOW_LINE_INFO);
        showLocals = options.modes.get(BCOConstants.F_SHOW_VARIABLES);
        showStackMap = options.modes.get(BCOConstants.F_SHOW_STACKMAP);
    }

    public CommentedASMifierClassVisitor(ClassNode classNode, final DecompilerOptions options) {
        this(options, "cw", 0);
    }

    @Override
    public void visit(int version, int access, String name1, String signature,
        String superName, String[] interfaces) {
        if(decompilingEntireClass()) {
            super.visit(version, access, name1, signature, superName, interfaces);
        }
        int major = version & 0xFFFF;
        //int minor = version >>> 16;
        // 1.1 is 45, 1.2 is 46 etc.
        int javaV = major % 44;
        if (javaV > 0 && javaV < 10) {
            javaVersion = "1." + javaV;
        }
        this.accessFlags = access;
    }

    private boolean decompilingEntireClass() {
        return options.methodFilter == null && options.fieldFilter == null;
    }

    @Override
    protected ASMifier createASMifier(String name1, int id1) {
        return new CommentedASMifierClassVisitor(options, name1, id1);
    }

    @Override
    public DecompiledClassInfo getClassInfo() {
        return new DecompiledClassInfo(javaVersion, accessFlags);
    }
}
