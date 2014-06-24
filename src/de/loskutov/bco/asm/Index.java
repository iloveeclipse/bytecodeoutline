package de.loskutov.bco.asm;

import org.objectweb.asm.tree.LabelNode;

/**
 * @author Eric Bruneton
 */

public class Index {

    public final LabelNode labelNode;

    public final int insn;

    public final int opcode;

    public Index(final LabelNode label, final int insn, final int opcode) {
        this.labelNode = label;
        this.insn = insn;
        this.opcode = opcode;
    }
}
