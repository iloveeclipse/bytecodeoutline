/*******************************************************************************
 * Copyright (c) 2011 Andrey Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrey Loskutov -
 * initial API and implementation
 *******************************************************************************/
package de.loskutov.bco.asm;

import java.util.BitSet;

public class DecompilerOptions {

    public final String fieldFilter;
    public final String methodFilter;
    public final BitSet modes;
    public final ClassLoader cl;

    public DecompilerOptions(final String fieldFilter, final String methodFilter,
        final BitSet modes, final ClassLoader cl) {
            this.fieldFilter = fieldFilter;
            this.methodFilter = methodFilter;
            this.modes = modes;
            this.cl = cl;
    }
}
