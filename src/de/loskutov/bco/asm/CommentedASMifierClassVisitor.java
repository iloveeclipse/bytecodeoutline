/*******************************************************************************
 * Copyright (c) 2004 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.bco.asm;

import java.util.BitSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.util.ASMifierClassVisitor;
import org.objectweb.asm.util.ASMifierMethodVisitor;

import de.loskutov.bco.preferences.BCOConstants;

/**
 * @author Andrei
 */
public class CommentedASMifierClassVisitor extends ASMifierClassVisitor {

    protected BitSet modes;
    protected boolean raw;

    protected ASMifierMethodVisitor createASMifierMethodVisitor() {
        return new ASMifierMethodVisitor() {

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

            public void visitLocalVariable(String name, String desc,
                String signature, Label start, Label end, int index) {
                if (showLocals) {
                    super.visitLocalVariable(
                        name, desc, signature, start, end, index);
                }
            }

            public void visitMaxs(int maxStack, int maxLocals) {
                if (showLocals) {
                    super.visitMaxs(maxStack, maxLocals);
                }
            }

        };
    }

    protected boolean showLines;
    protected boolean showLocals;
    protected boolean showStackMap;

    public CommentedASMifierClassVisitor(final BitSet modes) {
        super(null);
        this.modes = modes;
        raw = !modes.get(BCOConstants.F_SHOW_RAW_BYTECODE);
        showLines = modes.get(BCOConstants.F_SHOW_LINE_INFO);
        showLocals = modes.get(BCOConstants.F_SHOW_VARIABLES);
        showStackMap = modes.get(BCOConstants.F_SHOW_STACKMAP);
    }

    public void visitEnd() {
        text.add("cw.visitEnd();\n\n");
        text.add("return cw.toByteArray();\n");
        text.add("}\n");
        text.add("}\n");
    }
}