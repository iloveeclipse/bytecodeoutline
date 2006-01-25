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
import org.eclipse.jface.util.IPropertyChangeListener;
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

    private static final String ACTION = "action";
    private boolean isChecked;

    public DefaultToggleAction(String id, IPropertyChangeListener listener) {
        super();
        setId(id);
        init();

        IPreferenceStore store = BytecodeOutlinePlugin.getDefault().getPreferenceStore();

        isChecked = store.getBoolean(id);
        setChecked(isChecked);
        // as last action, after setChecked(), to prevent unexpected listener
        // events during initialization
        addPropertyChangeListener(listener);
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
        isChecked = !isChecked;
        setChecked(isChecked);
    }
}
