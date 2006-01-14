/*******************************************************************************
 * Copyright (c) 2006 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.bco.preferences;


/**
 * Keys for preferences store used in BCO
 * @author Andrei
 */
public interface BCOConstants {
    /**
     * toggle "view content/selection follows editor selection"
     */
    String LINK_VIEW_TO_EDITOR = "linkViewToEditor";

    /**
     * show bytecode only for selected element in editor
     */
    String SHOW_ONLY_SELECTED_ELEMENT = "showOnlySelectedElement";

    /**
     * show ASMifier java code instead of bytecode
     */
    String SHOW_ASMIFIER_CODE = "showASMifierCode";

    /**
     * show raw bytecode (without any additional help like readable class names etc)
     */
    String SHOW_RAW_BYTECODE = "showRawBytecode";

    /**
     * show line information (if available)
     */
    String SHOW_LINE_INFO = "showLineInfo";

    /**
     * show variables information (if available)
     */
    String SHOW_VARIABLES = "showVariables";

    /**
     * recalculate stackmap (to see computed frames, works for all classes even before MUSTANG)
     */
    String RECALCULATE_STACKMAP = "recalculateStackmap";

    /**
     * expand stackmap frames
     */
    String EXPAND_STACKMAP = "expandStackmap";

    /**
     * show "analyzer" - LVT and stack tables (for current bytecode selection)
     */
    String SHOW_ANALYZER = "showAnalyzer";

}
