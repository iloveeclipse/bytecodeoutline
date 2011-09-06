/*******************************************************************************
 * Copyright (c) 2011 Andrey Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrey Loskutov -
 * initial API and implementation
 *******************************************************************************/
package de.loskutov.bco.asm;

public class DecompiledClassInfo {
    public final String javaVersion;
    public final int accessFlags;

    public DecompiledClassInfo(String javaVersion, int accessFlags) {
        this.javaVersion = javaVersion;
        this.accessFlags = accessFlags;
    }
}
