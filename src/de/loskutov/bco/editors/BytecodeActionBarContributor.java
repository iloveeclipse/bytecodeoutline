
package de.loskutov.bco.editors;

import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorActionContributor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.loskutov.bco.BytecodeOutlinePlugin;


/**
 * Adds "Show Bytecode" action to tool/menu bars
 * 
 * @author V. Grishchenko, Eugene Kuleshov
 */
public class BytecodeActionBarContributor extends ClassFileEditorActionContributor {
    BytecodeClassFileEditor editor;
    private ShowBytecodeAction dAction;


    public BytecodeActionBarContributor() {
        String symbolicName = BytecodeOutlinePlugin.getDefault().getBundle().getSymbolicName();
        ImageDescriptor actionIcon = AbstractUIPlugin.imageDescriptorFromPlugin(symbolicName, "icons/bytecodeview.gif");
      
        dAction = new ShowBytecodeAction(actionIcon);
    }

    public void contributeToToolBar(IToolBarManager toolBarManager) {
        super.contributeToToolBar(toolBarManager);
        toolBarManager.add(dAction);
        // toolBarManager.add(new Separator(JadclipsePlugin.PID_JADCLIPSE));
        // toolBarManager.appendToGroup(JadclipsePlugin.PID_JADCLIPSE, dAction);
    }

    public void contributeToMenu(IMenuManager menu) {
        super.contributeToMenu(menu);
        IMenuManager edit = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (edit != null) {
            edit.add(dAction);
        }
    }

    public void setActiveEditor(IEditorPart targetEditor) {
        if (targetEditor instanceof BytecodeClassFileEditor) {
            editor = (BytecodeClassFileEditor) targetEditor;
            editor.doSetInput(false);
        } else {
            editor = null;
        }
        super.setActiveEditor(targetEditor);
    }


    private class ShowBytecodeAction extends Action {

      protected ShowBytecodeAction(ImageDescriptor actionIcon) {
          super("Show Bytecode@Ctrl+Shift+B", actionIcon);
          // setDescription("xxx");
          setToolTipText("Show Bytecode");
          setAccelerator(SWT.CTRL | SWT.SHIFT | 'B');
      }

      public void run() {
          if (editor != null) {
            editor.doSetInput(true);
        }
      }
  }

}

