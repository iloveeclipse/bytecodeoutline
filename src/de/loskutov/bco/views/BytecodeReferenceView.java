/*
 * Copyright area
 */

package de.loskutov.bco.views;

import java.net.URL;

import org.eclipse.help.internal.appserver.WebappManager;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;
import org.objectweb.asm.util.AbstractVisitor;

import de.loskutov.bco.BytecodeOutlinePlugin;


public class BytecodeReferenceView extends ViewPart implements IPartListener2, ISelectionListener {

    private static final String NLS_PREFIX = "BytecodeReferenceView.";
    private Browser browser;

    public void createPartControl(Composite parent) {
        browser = new Browser(parent, SWT.BORDER);
        browser.setText(BytecodeOutlinePlugin.getResourceString(NLS_PREFIX
            + "empty.selection.text"));
        getSite().getWorkbenchWindow().getPartService().addPartListener(this);

        // TODO run this in background!
        BaseHelpSystem.ensureWebappRunning();
    }

    public void dispose() {
        getSite().getWorkbenchWindow().getPartService().removePartListener(this);
        super.dispose();
    }

    public void setFocus() {
        /* nothing to do */
    }

    public void partActivated(IWorkbenchPartReference partRef) {
        //
    }

    public void partBroughtToTop(IWorkbenchPartReference partRef) {
        //
    }

    public void partClosed(IWorkbenchPartReference partRef) {
        //
    }

    public void partDeactivated(IWorkbenchPartReference partRef) {
        //
    }

    public void partOpened(IWorkbenchPartReference partRef) {
        // WORKAROUND  - sometimes Eclipse does not invoke partVisible(),
        // but only partOpened()...
        partVisible(partRef);
    }

    public void partHidden(IWorkbenchPartReference partRef) {
        if (partRef.getId().equals(getSite().getId())) {
            getSite().getWorkbenchWindow().getSelectionService()
                .removePostSelectionListener(BytecodeOutlineView.class.getName(), this);
        }
    }

    public void partVisible(IWorkbenchPartReference partRef) {
        if (partRef.getId().equals(getSite().getId())) {
            IWorkbenchWindow workbenchWindow = getSite().getWorkbenchWindow();
            ISelectionService selectionService = workbenchWindow
                .getSelectionService();
            String partId = BytecodeOutlineView.class.getName();
            selectionService.addPostSelectionListener(partId, this);

            // perform initialization with already existing selection (if any)
            ISelection selection = selectionService.getSelection(partId);
            if(selection != null) {
                IViewReference viewReference = workbenchWindow.getActivePage()
                    .findViewReference(partId);
                if(viewReference != null) {
                    selectionChanged(viewReference.getView(false), selection);
                }
            }
        }
    }

    public void partInputChanged(IWorkbenchPartReference partRef) {
        //
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if(!(part instanceof BytecodeOutlineView)){
            return;
        }
        int line = -1;
        String opcodeName = null;
        if (selection instanceof ITextSelection) {
            line = ((ITextSelection)selection).getStartLine();
        } else if(selection instanceof IStructuredSelection){
            IStructuredSelection sselection = (IStructuredSelection) selection;
            int size = sselection.size();
            if(size == 1 && sselection.getFirstElement() instanceof Integer){
                line = ((Integer)sselection.getFirstElement()).intValue();
            }
        }

        if(line >= 0){
            int opcode = ((BytecodeOutlineView)part).getBytecodeInstructionAtLine(line);
            if (opcode != -1) {
                opcodeName = AbstractVisitor.OPCODES[opcode];
            }
        }

        if (opcodeName != null) {
            opcodeName = checkOpcodeName(opcodeName);

            URL url = getHelpResource(opcodeName);
            if (url != null) {
                browser.setUrl(url.toString());
            } else {
                browser.setText(BytecodeOutlinePlugin.getResourceString(NLS_PREFIX
                    + "empty.selection.text"));
            }
        } else {
            browser.setText(BytecodeOutlinePlugin.getResourceString(NLS_PREFIX
                + "empty.selection.text"));
        }
    }

    private String checkOpcodeName(String opcodeName) {
        opcodeName = opcodeName.toLowerCase();
        /*
         * we need an additional check for DCONST_1...5, FCONST_1...5 etc case
         * to convert it to DCONST_D etc
         */
        int sepIndex = opcodeName.indexOf('_');
        if(sepIndex > 0 && Character.isDigit(opcodeName.charAt(sepIndex + 1))){
            opcodeName = opcodeName.substring(0, sepIndex);
            switch(opcodeName.charAt(0)){
                case 'd':
                    opcodeName += "_d";
                    break;
                case 'f':
                    opcodeName += "_f";
                    break;
                case 'l':
                    opcodeName += "_l";
                    break;
                default:
                    // ICONST uses "n"
                    opcodeName += "_n";
                    break;
            }
        }
        return opcodeName;
    }

    private URL getHelpResource(String name) {
        try {
            // BaseHelpSystem.resolve() method is not awailable in 3.0
            String host = WebappManager.getHost();
            int port = WebappManager.getPort();
            String href = "/"
                + BytecodeOutlinePlugin.getDefault().getBundle()
                    .getSymbolicName() + "/doc/ref-" + name + ".html";
            return new URL("http://" + host + ":" + port + "/help/nftopic"
                + href);
            // return BaseHelpSystem.resolve( href, true);
            // return new File(
            // BytecodeOutlinePlugin.PLUGIN_PATH+"/doc/ref-"+name.toLowerCase()+".html").toURL();

        } catch (Exception e) {
            return null;
        }
    }
}

