package de.loskutov.bco.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorActionContributor;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
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
    private final AttachSourceAction attachAction;

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

        actionIcon = AbstractUIPlugin.imageDescriptorFromPlugin(
            symbolicName, "icons/source.gif");
        attachAction = new AttachSourceAction(actionIcon);
    }

    @Override
    public void contributeToToolBar(IToolBarManager toolBarManager) {
        super.contributeToToolBar(toolBarManager);
        toolBarManager.add(refreshAction);
        toolBarManager.add(toggleRawBytecodeAction);
        toolBarManager.add(attachAction);
        // toolBarManager.add(new Separator(JadclipsePlugin.PID_JADCLIPSE));
        // toolBarManager.appendToGroup(JadclipsePlugin.PID_JADCLIPSE, dAction);
    }

    @Override
    public void contributeToMenu(IMenuManager menu) {
        super.contributeToMenu(menu);
        IMenuManager edit = menu
            .findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (edit != null) {
            edit.add(refreshAction);
            edit.add(toggleRawBytecodeAction);
            edit.add(attachAction);
        }
    }

    @Override
    public void setActiveEditor(IEditorPart targetEditor) {
        if (targetEditor instanceof BytecodeClassFileEditor) {
            editor = (BytecodeClassFileEditor) targetEditor;
            refreshAction.setEnabled(editor.hasMappedSource());
            refreshAction.setChecked(editor.isDecompiled());
            toggleRawBytecodeAction.setEnabled(editor.isDecompiled());
            toggleRawBytecodeAction.setChecked(editor
                .getDecompilerFlag(BCOConstants.F_SHOW_RAW_BYTECODE));
            attachAction.setEnabled(editor.isSourceAttachmentPossible());
        } else {
            refreshAction.setEnabled(false);
            toggleRawBytecodeAction.setEnabled(false);
            attachAction.setEnabled(false);
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

        @Override
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

    private class AttachSourceAction extends Action {

        protected AttachSourceAction(ImageDescriptor actionIcon) {
            super("Attach Source...", SWT.NONE);
            setImageDescriptor(actionIcon);
            setToolTipText("Attach Source...");
        }

        @Override
        public void run() {
            if (editor == null) {
                return;
            }
            IPackageFragmentRoot root = editor.getPackageFragmentRoot(editor.getClassFile());
            try {
                IClasspathEntry entry = root.getRawClasspathEntry();
                IPath containerPath = null;
                IJavaProject javaProject = root.getJavaProject();
                if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    containerPath = entry.getPath();
                    IClasspathContainer container = JavaCore
                        .getClasspathContainer(containerPath, javaProject);
                    if(!isSourceAttachmentPossible(containerPath, javaProject)){
                        editor.setSourceAttachmentPossible(false);
                        attachAction.setEnabled(false);
                        BytecodeOutlinePlugin
                            .error("Unable to configure source attachment:\n"
                                + "classpath entry '" + containerPath +
                                        "'\nis either read-only "
                                + "or source attachment is not supported...", null);
                        return;
                    }
                    entry = JavaModelUtil.findEntryInContainer(container, root
                        .getPath());
                }

                Shell shell = Display.getDefault().getActiveShell();
                IClasspathEntry cpe = BuildPathDialogAccess
                    .configureSourceAttachment(shell, entry);
                if (cpe == null) {
                    return;
                }
                String[] changedAttributes = {CPListElement.SOURCEATTACHMENT};
                BuildPathSupport.modifyClasspathEntry(
                    shell, cpe, changedAttributes, javaProject, containerPath,
                    cpe.getReferencingEntry() != null,
                    new NullProgressMonitor());
            } catch (CoreException e) {
                BytecodeOutlinePlugin.error(
                    "Unable to configure source attachment", e);
            }
        }

        private boolean isSourceAttachmentPossible(IPath containerPath,
            IJavaProject javaProject) {
            ClasspathContainerInitializer initializer = JavaCore
                .getClasspathContainerInitializer(containerPath
                    .segment(0));
            IStatus status = initializer.getSourceAttachmentStatus(
                containerPath, javaProject);
            if (status.getCode() == ClasspathContainerInitializer.ATTRIBUTE_NOT_SUPPORTED
                || status.getCode() == ClasspathContainerInitializer.ATTRIBUTE_READ_ONLY) {

                return false;
            }
            return true;
        }
    }

    private class ToggleRawBytecodeAction extends Action {

        protected ToggleRawBytecodeAction(ImageDescriptor actionIcon) {
            super("Show Internal Types", SWT.TOGGLE);
            setImageDescriptor(actionIcon);
            setToolTipText("Show Internal Types");
        }

        @Override
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
