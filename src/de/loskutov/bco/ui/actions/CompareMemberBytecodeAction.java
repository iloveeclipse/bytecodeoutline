/*****************************************************************************************
 * Copyright (c) 2011 Andrey Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrey Loskutov -
 * initial API and implementation
 ****************************************************************************************/
package de.loskutov.bco.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jface.action.IAction;

import de.loskutov.bco.BytecodeOutlinePlugin;

/**
 * @author Andrei
 */
public class CompareMemberBytecodeAction extends BytecodeAction {

    @Override
    public void run(IAction action) {
        IJavaElement[] resources = getSelectedResources();
        try {
            exec(resources[0], resources[1]);
        } catch (Exception e) {
            BytecodeOutlinePlugin.error("Failed to run Compare: "
                + e.getMessage(), e);
        }
    }

    @Override
    protected IJavaElement[] getSelectedResources() {
        ArrayList<Object> resources = null;
        if (!selection.isEmpty()) {
            resources = new ArrayList<Object>();
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
            return resources.toArray(new IJavaElement[resources
                .size()]);
        }

        return new IJavaElement[0];
    }
}
