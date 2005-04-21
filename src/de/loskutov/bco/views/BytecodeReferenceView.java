/*
 * Copyright area
 */

package de.loskutov.bco.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;

import org.objectweb.asm.util.AbstractVisitor;

import de.loskutov.bco.BytecodeOutlinePlugin;


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

            String opcodeName = null;
            if (opcode != -1) {
                opcodeName = AbstractVisitor.OPCODES[opcode];
            }

            if (opcodeName != null) {
                URL url = getHelpResource( opcodeName);
                if( url!=null) {
                    browser.setText( getContent( url));
                } else {
                    browser.setText("");
                }
            } else {
                browser.setText("");
            }
        }
    }
    
    private String getContent(URL url) {
      BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(url.openStream()));
            if (r == null)
                return null;

            StringBuffer sb = new StringBuffer();
            String l;
            while ((l = r.readLine()) != null) {
                sb.append(l).append("\n");
            }
            return sb.toString();

        } catch (IOException ex) {
            //
        } finally {
            try {
                r.close();
            } catch (IOException e) {
                //
            }
        }
        return null;
    }

    private URL getHelpResource( String name) {
      String pluginId = BytecodeOutlinePlugin.getDefault().getBundle().getSymbolicName();
      Bundle bundle = Platform.getBundle(pluginId);
      
      int state = bundle.getState();  // verify if bundle is ready
      if(( state & ( Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING))==0) {
          return null;
      }

      return Platform.find(bundle, new Path( "doc/ref-"+name.toLowerCase()+".html"));
    }
}

