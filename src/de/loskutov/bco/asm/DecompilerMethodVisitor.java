package de.loskutov.bco.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.AbstractVisitor;

/**
 * @author Eric Bruneton
 */

public class DecompilerMethodVisitor extends MethodAdapter {

    private String owner;

    private List text;

    private MethodNode meth;

    private Label currentLabel;

    private int currentInsn;

    private Map lineNumbers;

    private List localVariables;

    public DecompilerMethodVisitor(final String owner, final MethodNode meth,
        final MethodVisitor mv) {
        super(mv);
        this.owner = owner;
        this.text = ((AbstractVisitor) mv).getText();
        this.meth = meth;
        this.lineNumbers = new HashMap();
        this.localVariables = new ArrayList();
    }

    public DecompiledMethod getResult(final ClassLoader cl) {
        return new DecompiledMethod(
            owner, text, lineNumbers, localVariables, meth, cl);
    }

    public void visitInsn(final int opcode) {
        addIndex(opcode);
        super.visitInsn(opcode);
        if (meth != null) {
            meth.visitInsn(opcode);
        }
    }

    public void visitIntInsn(final int opcode, final int operand) {
        addIndex(opcode);
        super.visitIntInsn(opcode, operand);
        if (meth != null) {
            meth.visitIntInsn(opcode, operand);
        }
    }

    public void visitVarInsn(final int opcode, final int var) {
        addIndex(opcode);
        super.visitVarInsn(opcode, var);
        if (meth != null) {
            meth.visitVarInsn(opcode, var);
        }
    }

    public void visitTypeInsn(final int opcode, final String desc) {
        addIndex(opcode);
        super.visitTypeInsn(opcode, desc);
        if (meth != null) {
            meth.visitTypeInsn(opcode, desc);
        }
    }

    public void visitFieldInsn(final int opcode, final String owner1,
        final String name, final String desc) {
        addIndex(opcode);
        super.visitFieldInsn(opcode, owner1, name, desc);
        if (meth != null) {
            meth.visitFieldInsn(opcode, owner1, name, desc);
        }
    }

    public void visitMethodInsn(final int opcode, final String owner1,
        final String name, final String desc) {
        addIndex(opcode);
        super.visitMethodInsn(opcode, owner1, name, desc);
        if (meth != null) {
            meth.visitMethodInsn(opcode, owner1, name, desc);
        }
    }

    public void visitJumpInsn(final int opcode, final Label label) {
        addIndex(opcode);
        super.visitJumpInsn(opcode, label);
        if (meth != null) {
            meth.visitJumpInsn(opcode, label);
        }
    }

    public void visitLabel(final Label label) {
        currentLabel = label;
        addIndex(-1);
        super.visitLabel(label);
        if (meth != null) {
            meth.visitLabel(label);
        }
    }

    public void visitLdcInsn(final Object cst) {
        addIndex(Opcodes.LDC);
        super.visitLdcInsn(cst);
        if (meth != null) {
            meth.visitLdcInsn(cst);
        }
    }

    public void visitIincInsn(final int var, final int increment) {
        addIndex(Opcodes.IINC);
        super.visitIincInsn(var, increment);
        if (meth != null) {
            meth.visitIincInsn(var, increment);
        }
    }

    public void visitTableSwitchInsn(final int min, final int max,
        final Label dflt, final Label[] labels) {
        addIndex(Opcodes.TABLESWITCH);
        super.visitTableSwitchInsn(min, max, dflt, labels);
        if (meth != null) {
            meth.visitTableSwitchInsn(min, max, dflt, labels);
        }
    }

    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
        final Label[] labels) {
        addIndex(Opcodes.LOOKUPSWITCH);
        super.visitLookupSwitchInsn(dflt, keys, labels);
        if (meth != null) {
            meth.visitLookupSwitchInsn(dflt, keys, labels);
        }
    }

    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        addIndex(Opcodes.MULTIANEWARRAY);
        super.visitMultiANewArrayInsn(desc, dims);
        if (meth != null) {
            meth.visitMultiANewArrayInsn(desc, dims);
        }
    }

    public void visitTryCatchBlock(final Label start, final Label end,
        final Label handler, final String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        if (meth != null) {
            meth.visitTryCatchBlock(start, end, handler, type);
        }
    }

    public void visitLocalVariable(final String name, final String desc,
        final String signature, final Label start, final Label end,
        final int index) {
        localVariables.add(new LocalVariableNode(
            name, desc, signature, start, end, index));
        super.visitLocalVariable(name, desc, signature, start, end, index);
        if (meth != null) {
            meth.visitLocalVariable(name, desc, signature, start, end, index);
        }
    }

    public void visitLineNumber(final int line, final Label start) {
        lineNumbers.put(start, new Integer(line));
        super.visitLineNumber(line, start);
        if (meth != null) {
            meth.visitLineNumber(line, start);
        }
    }

    public void visitMaxs(final int maxStack, final int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
        if (meth != null) {
            meth.visitMaxs(maxStack, maxLocals);
        }
    }

    protected void addIndex(final int opcode) {
        text.add(new Index(currentLabel, currentInsn++, opcode));
    }
}
