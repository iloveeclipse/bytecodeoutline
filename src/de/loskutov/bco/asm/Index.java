package de.loskutov.bco.asm;

import org.objectweb.asm.Label;

/**
 * @author Eric Bruneton
 */

public class Index {

    public final Label label;

    public final int insn;

    public Index(final Label label, final int insn) {
        this.label = label;
        this.insn = insn;
    }
}
