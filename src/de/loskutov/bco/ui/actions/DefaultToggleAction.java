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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.loskutov.bco.BytecodeOutlinePlugin;


/**
 * Default action which could be used as template for "toggle" action.
 * Action image, text and tooltip will be initialized by default.
 * To use it, register IPropertyChangeListener and check for IAction.CHECKED
 * event name.
 * @author Andrei
 */
public abstract class DefaultToggleAction extends Action {

    private static final String ACTION = "action";

    public DefaultToggleAction(String id) {
        super();
        setId(id);
        init();

        IPreferenceStore store = BytecodeOutlinePlugin.getDefault().getPreferenceStore();

        boolean isChecked = store.getBoolean(id);
        setChecked(isChecked);
    }

    private void init(){
        String myId = getId();
        if(myId != null && myId.startsWith("diff_")) {
            myId = myId.substring("diff_".length());
        }
        setImageDescriptor(AbstractUIPlugin
            .imageDescriptorFromPlugin(
                BytecodeOutlinePlugin.getDefault().getBundle()
                    .getSymbolicName(),
                BytecodeOutlinePlugin
                    .getResourceString(ACTION + "." + myId + "." + IMAGE)));

        setText(BytecodeOutlinePlugin
            .getResourceString(ACTION + "." + myId + "." + TEXT));
        setToolTipText(BytecodeOutlinePlugin
            .getResourceString(ACTION + "." + myId + "." + TOOL_TIP_TEXT));
    }

    /**
     * @see org.eclipse.jface.action.IAction#run()
     */
    public final void run() {
        boolean isChecked = isChecked();
        IPreferenceStore store = BytecodeOutlinePlugin.getDefault().getPreferenceStore();
        store.setValue(getId(), isChecked);
        run(isChecked);
    }

    public abstract void run(boolean newState);
}
