/*****************************************************************************************
 * Copyright (c) 2004 Andrei Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrei Loskutov -
 * initial API and implementation
 ****************************************************************************************/
package de.loskutov.bco.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IObjectActionDelegate;

import de.loskutov.bco.BytecodeOutlinePlugin;

/**
 * @author Andrei
 */
public class CompareMemberBytecodeAction extends BytecodeAction implements IObjectActionDelegate {

    /**
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        IJavaElement[] resources = getSelectedResources();
        try {
            exec(resources[0], resources[1]);
        } catch (Exception e) {
            BytecodeOutlinePlugin.error("Failed to run Compare: "
                + e.getMessage(), e);
        }
    }


    protected IJavaElement[] getSelectedResources() {
        ArrayList resources = null;
        if (!selection.isEmpty()) {
            resources = new ArrayList();
            for (Iterator elements = selection.iterator(); elements.hasNext();) {
                Object next = elements.next();
                if (next instanceof IMember) {
                    resources.add(next);
                    continue;
                } else if (next instanceof IAdaptable) {
                    IAdaptable a = (IAdaptable) next;
                    Object adapter = a.getAdapter(IMember.class);
                    if (adapter instanceof IMember) {
                        resources.add(adapter);
                        continue;
                    }
                }
            }
        }

        if (resources != null && !resources.isEmpty()) {
            return (IJavaElement[]) resources.toArray(new IJavaElement[resources
                .size()]);
        }

        return new IJavaElement[0];
    }
}