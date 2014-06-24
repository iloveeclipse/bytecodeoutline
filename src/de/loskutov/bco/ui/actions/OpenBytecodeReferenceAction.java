/*****************************************************************************************
 * Copyright (c) 2007 Andrei Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrei Loskutov -
 * initial API and implementation
 ****************************************************************************************/
package de.loskutov.bco.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.loskutov.bco.BytecodeOutlinePlugin;

/**
 * Opens a bytecode reference from view
 * @author Andrei
 */
public class OpenBytecodeReferenceAction implements IViewActionDelegate {

    public OpenBytecodeReferenceAction() {
        super();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().showView(
                    "de.loskutov.bco.views.BytecodeReferenceView");
        } catch (PartInitException e) {
            BytecodeOutlinePlugin.error(
                "Could not open Bytecode Reference View: " + e.getMessage(), e);
        }

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
     * org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        // no op
    }

    @Override
    public void init(IViewPart view) {
        // no op
    }

}
