package de.loskutov.bco.asm;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.AbstractVisitor;

import de.loskutov.bco.asm.CommentedClassVisitor.CommentedAnnotationVisitor;

/**
 * @author Eric Bruneton
 */

public class DecompilerMethodVisitor extends MethodAdapter {

    private String owner;

    private List text;

    private MethodNode meth;

    private LabelNode currentLabel;

    private int currentInsn;

    private Map lineNumbers;

    private final BitSet modes;

    public DecompilerMethodVisitor(final String owner, final MethodNode meth,
        final MethodVisitor mv, BitSet modes) {
        super(mv);
        this.owner = owner;
        this.modes = modes;
        this.text = ((AbstractVisitor) mv).getText();
        this.meth = meth;
        this.lineNumbers = new HashMap();
    }

    public DecompiledMethod getResult(final ClassLoader cl) {
        return new DecompiledMethod(owner, text, lineNumbers, meth, cl, modes);
    }

    public AnnotationVisitor visitAnnotationDefault() {
        AnnotationVisitor annVisitor = super.visitAnnotationDefault();
        AnnotationVisitor visitor = meth.visitAnnotationDefault();
        if (annVisitor instanceof CommentedAnnotationVisitor) {
            CommentedAnnotationVisitor av = (CommentedAnnotationVisitor) annVisitor;
            av.setAnnotationVisitor(visitor);
        }
        return annVisitor;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor annVisitor = super.visitAnnotation(desc, visible);
        AnnotationVisitor visitor = meth.visitAnnotation(desc, visible);
        if (annVisitor instanceof CommentedAnnotationVisitor) {
            CommentedAnnotationVisitor av = (CommentedAnnotationVisitor) annVisitor;
            av.setAnnotationVisitor(visitor);
        }
        return annVisitor;
    }

    public AnnotationVisitor visitParameterAnnotation(int parameter,
        String desc, boolean visible) {
        AnnotationVisitor annVisitor = super.visitParameterAnnotation(
            parameter, desc, visible);
        AnnotationVisitor visitor = meth.visitParameterAnnotation(
            parameter, desc, visible);
        if (annVisitor instanceof CommentedAnnotationVisitor) {
            CommentedAnnotationVisitor av = (CommentedAnnotationVisitor) annVisitor;
            av.setAnnotationVisitor(visitor);
        }
        return annVisitor;
    }

    public void visitCode() {
        super.visitCode();
        meth.visitCode();
    }

    public void visitEnd() {
        super.visitEnd();
        meth.visitEnd();
    }

    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
        meth.visitAttribute(attr);
    }

    public void visitInsn(final int opcode) {
        addIndex(opcode);
        super.visitInsn(opcode);
        meth.visitInsn(opcode);
    }

    public void visitIntInsn(final int opcode, final int operand) {
        addIndex(opcode);
        super.visitIntInsn(opcode, operand);
        meth.visitIntInsn(opcode, operand);
    }

    public void visitVarInsn(final int opcode, final int var) {
        addIndex(opcode);
        super.visitVarInsn(opcode, var);
        meth.visitVarInsn(opcode, var);
    }

    public void visitTypeInsn(final int opcode, final String desc) {
        addIndex(opcode);
        super.visitTypeInsn(opcode, desc);
        meth.visitTypeInsn(opcode, desc);
    }

    public void visitFieldInsn(final int opcode, final String owner1,
        final String name, final String desc) {
        addIndex(opcode);
        super.visitFieldInsn(opcode, owner1, name, desc);
        meth.visitFieldInsn(opcode, owner1, name, desc);
    }

    public void visitMethodInsn(final int opcode, final String owner1,
        final String name, final String desc) {
        addIndex(opcode);
        super.visitMethodInsn(opcode, owner1, name, desc);
        meth.visitMethodInsn(opcode, owner1, name, desc);
    }

    public void visitJumpInsn(final int opcode, final Label label) {
        addIndex(opcode);
        super.visitJumpInsn(opcode, label);
        meth.visitJumpInsn(opcode, label);
    }

    public void visitLabel(final Label label) {
        super.visitLabel(label);
        meth.visitLabel(label);
        currentLabel = (LabelNode) meth.instructions.getLast();
        addIndex(-1);
    }

    public void visitLdcInsn(final Object cst) {
        addIndex(Opcodes.LDC);
        super.visitLdcInsn(cst);
        meth.visitLdcInsn(cst);
    }

    public void visitIincInsn(final int var, final int increment) {
        addIndex(Opcodes.IINC);
        super.visitIincInsn(var, increment);
        meth.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(final int min, final int max,
        final Label dflt, final Label[] labels) {
        addIndex(Opcodes.TABLESWITCH);
        super.visitTableSwitchInsn(min, max, dflt, labels);
        meth.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
        final Label[] labels) {
        addIndex(Opcodes.LOOKUPSWITCH);
        super.visitLookupSwitchInsn(dflt, keys, labels);
        meth.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        addIndex(Opcodes.MULTIANEWARRAY);
        super.visitMultiANewArrayInsn(desc, dims);
        meth.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitTryCatchBlock(final Label start, final Label end,
        final Label handler, final String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        meth.visitTryCatchBlock(start, end, handler, type);
    }

    public void visitLocalVariable(final String name, final String desc,
        final String signature, final Label start, final Label end,
        final int index) {
        // localVariables.add(new LocalVariableNode(
        // name, desc, signature, new LabelNode(start), new LabelNode(end), index));
        super.visitLocalVariable(name, desc, signature, start, end, index);
        meth.visitLocalVariable(name, desc, signature, start, end, index);
    }

    public void visitLineNumber(final int line, final Label start) {
        lineNumbers.put(start, new Integer(line));
        super.visitLineNumber(line, start);
        meth.visitLineNumber(line, start);
    }

    public void visitMaxs(final int maxStack, final int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
        meth.visitMaxs(maxStack, maxLocals);
    }

    public void visitFrame(int type, int nLocal, Object[] local, int nStack,
        Object[] stack) {
        addIndex(-1);
        super.visitFrame(type, nLocal, local, nStack, stack);
        meth.visitFrame(type, nLocal, local, nStack, stack);
    }

    protected void addIndex(final int opcode) {
        text.add(new Index(currentLabel, currentInsn++, opcode));
    }
}
