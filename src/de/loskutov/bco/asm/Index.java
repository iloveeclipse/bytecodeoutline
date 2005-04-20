package de.loskutov.bco.asm;

import org.objectweb.asm.Label;

/**
 * @author Eric Bruneton
 */

public class Index {

    public final Label label;

    public final int insn;
    
    public final int opcode;

    public Index(final Label label, final int insn, final int opcode) {
        this.label = label;
        this.insn = insn;
        this.opcode = opcode;
    }
}
