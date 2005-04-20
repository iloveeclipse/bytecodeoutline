/*
 * Copyright area
 */

package de.loskutov.bco.views;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.objectweb.asm.util.AbstractVisitor;


public class BytecodeReferenceView extends ViewPart implements ISelectionListener {

    Browser browser;
    
    public void createPartControl(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
        getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(this);
    }
    
    public void setFocus() {
        /* nothing to do */
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof ITextSelection && part instanceof BytecodeOutlineView) {
            int line = ((ITextSelection)selection).getStartLine();
            int opcode = ((BytecodeOutlineView)part).getBytecodeInstructionAtLine(line);
            /* TODO replace code below with something that gets HTML content from opcode */
            String opcodeName = null;
            if (opcode != -1) {
                opcodeName = AbstractVisitor.OPCODES[opcode];
            }
            if (opcodeName != null) {
                browser.setText("<html><body>" + opcodeName + "</body></html>");
            } else {
                browser.setText("");
            }
        }
    }
}
