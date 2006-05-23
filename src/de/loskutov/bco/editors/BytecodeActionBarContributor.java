package de.loskutov.bco.editors;

import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorActionContributor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.loskutov.bco.BytecodeOutlinePlugin;
import de.loskutov.bco.preferences.BCOConstants;

/**
 * Adds "Show Bytecode" action to tool/menu bars
 * @author V. Grishchenko, Eugene Kuleshov, Andrei
 */
public class BytecodeActionBarContributor
    extends
        ClassFileEditorActionContributor {

    BytecodeClassFileEditor editor;
    protected ShowBytecodeAction refreshAction;
    protected ToggleRawBytecodeAction toggleRawBytecodeAction;

    public BytecodeActionBarContributor() {
        super();
        String symbolicName = BytecodeOutlinePlugin.getDefault().getBundle()
            .getSymbolicName();
        ImageDescriptor actionIcon = AbstractUIPlugin
            .imageDescriptorFromPlugin(symbolicName, "icons/bytecodeview.gif");

        refreshAction = new ShowBytecodeAction(actionIcon);

        actionIcon = AbstractUIPlugin.imageDescriptorFromPlugin(
            symbolicName, "icons/raw_mode.gif");
        toggleRawBytecodeAction = new ToggleRawBytecodeAction(actionIcon);

    }

    public void contributeToToolBar(IToolBarManager toolBarManager) {
        super.contributeToToolBar(toolBarManager);
        toolBarManager.add(refreshAction);
        toolBarManager.add(toggleRawBytecodeAction);
        // toolBarManager.add(new Separator(JadclipsePlugin.PID_JADCLIPSE));
        // toolBarManager.appendToGroup(JadclipsePlugin.PID_JADCLIPSE, dAction);
    }

    public void contributeToMenu(IMenuManager menu) {
        super.contributeToMenu(menu);
        IMenuManager edit = menu
            .findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (edit != null) {
            edit.add(refreshAction);
            edit.add(toggleRawBytecodeAction);
        }
    }

    public void setActiveEditor(IEditorPart targetEditor) {
        if (targetEditor instanceof BytecodeClassFileEditor) {
            editor = (BytecodeClassFileEditor) targetEditor;
            refreshAction.setEnabled(editor.hasMappedSource());
            refreshAction.setChecked(editor.isDecompiled());
            toggleRawBytecodeAction.setEnabled(editor.isDecompiled());
            toggleRawBytecodeAction.setChecked(editor
                .getDecompilerFlag(BCOConstants.F_SHOW_RAW_BYTECODE));
        } else {
            refreshAction.setEnabled(false);
            toggleRawBytecodeAction.setEnabled(false);
            editor = null;
        }
        super.setActiveEditor(targetEditor);
    }

    private class ShowBytecodeAction extends Action {

        protected ShowBytecodeAction(ImageDescriptor actionIcon) {
            super("Show Bytecode@Ctrl+Shift+B", SWT.TOGGLE);
            setImageDescriptor(actionIcon);
            setToolTipText("Show Bytecode");
            setAccelerator(SWT.CTRL | SWT.SHIFT | 'B');
        }

        public void run() {
            if (editor != null) {
                ISelection selection = editor.getSelectionProvider()
                    .getSelection();
                boolean showBytecode = isChecked();
                if (editor.isDecompiled() != showBytecode) {

                    editor.doSetInput(true, !showBytecode);

                    if (selection instanceof ITextSelection) {
                        ITextSelection textSelection = (ITextSelection) selection;
                        textSelection = editor.convertSelection(
                            textSelection, showBytecode);
                        editor.getSelectionProvider().setSelection(
                            textSelection);
                    }
                }
                toggleRawBytecodeAction.setEnabled(editor.isDecompiled());
            }
        }
    }

    private class ToggleRawBytecodeAction extends Action {

        protected ToggleRawBytecodeAction(ImageDescriptor actionIcon) {
            super("Show Internal Types", SWT.TOGGLE);
            setImageDescriptor(actionIcon);
            setToolTipText("Show Internal Types");
        }

        public void run() {
            if (editor != null) {
                editor.setDecompilerFlag(
                    BCOConstants.F_SHOW_RAW_BYTECODE, isChecked());
                ISelection selection = editor.getSelectionProvider()
                    .getSelection();

                // we convert selection first to source line bacause bytecode lines could
                // not match for different bytecode view modes.
                int sourceLine = 0;
                if (selection instanceof ITextSelection) {
                    sourceLine = editor.getSourceLine((ITextSelection) selection);
                }

                editor.doSetInput(true, false);

                if (selection instanceof ITextSelection) {
                    ITextSelection textSelection = editor.convertLine(sourceLine);
                    if(textSelection != null) {
                        editor.getSelectionProvider().setSelection(textSelection);
                    }
                }
            }
        }
    }

}
