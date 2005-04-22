/*
 * Copyright area
 */

package de.loskutov.bco.views;

import java.net.URL;

import org.eclipse.help.internal.appserver.WebappManager;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.objectweb.asm.util.AbstractVisitor;

import de.loskutov.bco.BytecodeOutlinePlugin;


public class BytecodeReferenceView extends ViewPart implements IPartListener2, ISelectionListener {

    Browser browser;

    public void createPartControl(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
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
            getSite().getWorkbenchWindow().getSelectionService().removePostSelectionListener(this);
        }
    }

    public void partVisible(IWorkbenchPartReference partRef) {
        if (partRef.getId().equals(getSite().getId())) {
            getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(this);
        }
    }

    public void partInputChanged(IWorkbenchPartReference partRef) {
        //
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof ITextSelection && part instanceof BytecodeOutlineView) {
            int line = ((ITextSelection)selection).getStartLine();
            int opcode = ((BytecodeOutlineView)part).getBytecodeInstructionAtLine(line);

            String opcodeName = null;
            if (opcode != -1) {
                opcodeName = AbstractVisitor.OPCODES[opcode];
            }

            if (opcodeName != null) {
                URL url = getHelpResource( opcodeName);
                if( url!=null) {
                    browser.setUrl( url.toString());
                } else {
                    browser.setText("");
                }
            } else {
                browser.setText("");
            }
        }
    }

    private URL getHelpResource( String name) {
      try {
        // BaseHelpSystem.resolve() method is not awailable in 3.0
        String host = WebappManager.getHost();
        int port = WebappManager.getPort();
        String href = "/"+BytecodeOutlinePlugin.getDefault().getBundle().getSymbolicName()+
            "/doc/ref-"+name.toLowerCase()+".html";
        return new URL( "http://"+host+":"+port+"/help/nftopic"+href);
        // return BaseHelpSystem.resolve( href, true);        
        // return new File( BytecodeOutlinePlugin.PLUGIN_PATH+"/doc/ref-"+name.toLowerCase()+".html").toURL();
        
      } catch( Exception e) {
        return null;
      }
    }
}

