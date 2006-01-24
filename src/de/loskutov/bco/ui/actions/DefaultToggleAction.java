/*******************************************************************************
 * Copyright (c) 2006 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.bco.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.loskutov.bco.BytecodeOutlinePlugin;


/**
 * Default action which could be used as template for "toggle" action.
 * Action image, text and tooltip will be initialized by default.
 * To use it, register IPropertyChangeListener and check for IAction.CHECKED
 * event name.
 * @author Andrei
 */
public class DefaultToggleAction extends Action {
    /** Action id for toggle asmifier mode on/off */
    public static final String TOGGLE_ASMIFIER = "toggle_asmifier_mode";
    /** Action id for hide/show line info */
    public static final String HIDE_LINE_INFO = "hide_line_info";
    /** Action id for hide/show local variables */
    public static final String HIDE_LOCALS = "hide_locals";
    /** Action id for hide/show verifier */
    public static final String SHOW_VERIFIER = "show_verifier";
    /** Action id for hide/show raw bytecode */
    public static final String SHOW_RAW_BYTECODE = "show_raw_mode";
    /** Action id for show selected element only */
    public static final String SHOW_SELECTED_ONLY = "show_selected_only";
    /** Action id for link with editor mode on/off */
    public static final String LINK_WITH_EDITOR = "link_with_editor";
    /**
     * used to get text from props file
     */
    private static final String ACTION = "action";
    private boolean checked;

    public DefaultToggleAction(String id, boolean isChecked){
        super();
        checked = isChecked;
        // to init internal "value" attribute in Action class to proper start value
        setChecked(checked);
        setId(id);
        init();
    }

    private void init(){
        setImageDescriptor(AbstractUIPlugin
            .imageDescriptorFromPlugin(
                BytecodeOutlinePlugin.getDefault().getBundle()
                    .getSymbolicName(),
                BytecodeOutlinePlugin
                    .getResourceString(ACTION + "." + getId() + "." + IMAGE)));

        setText(BytecodeOutlinePlugin
            .getResourceString(ACTION + "." + getId() + "." + TEXT));
        setToolTipText(BytecodeOutlinePlugin
            .getResourceString(ACTION + "." + getId() + "." + TOOL_TIP_TEXT));
    }

    /**
     * @see org.eclipse.jface.action.IAction#run()
     */
    public void run() {
        checked = !checked;
        setChecked(checked);
    }
}
