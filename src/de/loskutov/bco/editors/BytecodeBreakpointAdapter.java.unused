package de.loskutov.bco.editors;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.BreakpointLocationVerifierJob;
import org.eclipse.jdt.internal.debug.ui.actions.ToggleBreakpointAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;


/**
 * Extend ToggleBreakpointAdapter to allow us map source code lines to the bytecode lines
 * TODO implement the mapping :)
 * @author Andrei
 */
public class BytecodeBreakpointAdapter extends ToggleBreakpointAdapter {

    public boolean canToggleBreakpoints(IWorkbenchPart part,
        ISelection selection) {
        // should work for us.
        return super.canToggleBreakpoints(part, selection);
    }

    public void toggleBreakpoints(IWorkbenchPart part, ISelection selection)
        throws CoreException {
        // should work for us.
        super.toggleBreakpoints(part, selection);
    }

    public boolean canToggleLineBreakpoints(IWorkbenchPart part,
        ISelection selection) {
        // should work for us.
        return super.canToggleBreakpoints(part, selection);
    }

    public boolean canToggleMethodBreakpoints(IWorkbenchPart part,
        ISelection selection) {
        // TODO should revisit, because it deals with IJavaElements in the selection
        return super.canToggleMethodBreakpoints(part, selection);
    }

    public boolean canToggleWatchpoints(IWorkbenchPart part,
        ISelection selection) {
        // TODO should revisit, because it deals with IJavaElements in the selection
        return super.canToggleWatchpoints(part, selection);
    }

    public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection)
        throws CoreException {
        // should work for us.
        super.toggleLineBreakpoints(part, selection);
    }

    public void toggleMethodBreakpoints(final IWorkbenchPart part,
        final ISelection finalSelection)  {

        if(part instanceof BytecodeClassFileEditor){
            BytecodeClassFileEditor classEditor = (BytecodeClassFileEditor) part;
            if(classEditor.isDecompiled()){
                return;
            }
            super.toggleMethodBreakpoints(part, finalSelection);
        }
    }

    /**
     * Toggles a line breakpoint. This is also the method called by the keybinding for creating breakpoints
     * @param part the currently active workbench part
     * @param selection the current selection
     * @param bestMatch if we should make a best match or not
     */
    public void toggleLineBreakpoints(final IWorkbenchPart part, final ISelection selection, final boolean bestMatch) {
        if(part instanceof BytecodeClassFileEditor){
            BytecodeClassFileEditor classEditor = (BytecodeClassFileEditor) part;
            if(classEditor.isDecompiled()){
                return;
            }
            super.toggleLineBreakpoints(part, selection, bestMatch);
        }
    }

    /**
     * Returns any existing method breakpoint for the specified method or <code>null</code> if none.
     *
     * @param typeName fully qualified type name
     * @param methodName method selector
     * @param methodSignature method signature
     * @return existing method or <code>null</code>
     * @throws CoreException
     */
    protected IJavaMethodBreakpoint getMethodBreakpoint(String typeName, String methodName, String methodSignature) throws CoreException {
        final IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
        IBreakpoint[] breakpoints = breakpointManager.getBreakpoints(JDIDebugModel.getPluginIdentifier());
        for (int i = 0; i < breakpoints.length; i++) {
            IBreakpoint breakpoint = breakpoints[i];
            if (breakpoint instanceof IJavaMethodBreakpoint) {
                final IJavaMethodBreakpoint methodBreakpoint = (IJavaMethodBreakpoint) breakpoint;
                if (typeName.equals(methodBreakpoint.getTypeName()) && methodName.equals(methodBreakpoint.getMethodName()) && methodSignature.equals(methodBreakpoint.getMethodSignature())) {
                    return methodBreakpoint;
                }
            }
        }
        return null;
    }

    /**
     * Returns any existing watchpoint for the given field, or <code>null</code> if none.
     *
     * @param typeName fully qualified type name on which watchpoint may exist
     * @param fieldName field name
     * @return any existing watchpoint for the given field, or <code>null</code> if none
     * @throws CoreException
     */
    protected IJavaWatchpoint getWatchpoint(String typeName, String fieldName) throws CoreException {
        IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
        IBreakpoint[] breakpoints = breakpointManager.getBreakpoints(JDIDebugModel.getPluginIdentifier());
        for (int i = 0; i < breakpoints.length; i++) {
            IBreakpoint breakpoint = breakpoints[i];
            if (breakpoint instanceof IJavaWatchpoint) {
                IJavaWatchpoint watchpoint = (IJavaWatchpoint) breakpoint;
                if (typeName.equals(watchpoint.getTypeName()) && fieldName.equals(watchpoint.getFieldName())) {
                    return watchpoint;
                }
            }
        }
        return null;
    }

    protected void createMethodBreakpoint(IResource resource, String typeName, String methodName, String methodSignature, boolean entry, boolean exit, boolean nativeOnly, int lineNumber, int charStart, int charEnd, int hitCount, boolean register, Map attributes) throws CoreException {
        JDIDebugModel.createMethodBreakpoint(resource, typeName, methodName, methodSignature, entry, exit, nativeOnly, lineNumber, charStart, charEnd, hitCount, register, attributes);
    }

    protected void createWatchpoint(IResource resource, String typeName, String fieldName, int lineNumber, int charStart, int charEnd, int hitCount, boolean register, Map attributes) throws CoreException {
        JDIDebugModel.createWatchpoint(resource, typeName, fieldName, lineNumber, charStart, charEnd, hitCount, register, attributes);
    }

    protected void createLineBreakpoint(IResource resource, String typeName, int lineNumber, int charStart, int charEnd, int hitCount, boolean register, Map attributes, IDocument document, boolean bestMatch, IType type, IEditorPart editorPart) throws CoreException {
        IJavaLineBreakpoint breakpoint = JDIDebugModel.createLineBreakpoint(resource, typeName, lineNumber, charStart, charEnd, hitCount, register, attributes);
        new BreakpointLocationVerifierJob(document, breakpoint, lineNumber, bestMatch, typeName, type, resource, editorPart).schedule();
    }

    /**
     * Removes the specified breakpoint
     * @param breakpoint the breakpoint to remove
     * @param delete if it should be deleted as well
     * @throws CoreException
     */
    protected void removeBreakpoint(IBreakpoint breakpoint, boolean delete) throws CoreException {
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpoint(breakpoint, delete);
    }

    /**
     * Returns if the structured selection is itself or is part of an interface
     * @param selection the current selection
     * @return true if the selection isor is part of an interface, false otherwise
     * @since 3.2
     */
    protected boolean isInterface(ISelection selection) {
        if (!selection.isEmpty()) {
            try {
                if(selection instanceof IStructuredSelection) {
                    IStructuredSelection ss = (IStructuredSelection) selection;
                    Iterator iterator = ss.iterator();
                    IType type = null;
                    Object obj = null;
                    while (iterator.hasNext()) {
                        obj = iterator.next();
                        if(obj instanceof IMember) {
                            type = ((IMember)obj).getDeclaringType();
                        }
                        if(type != null && type.isInterface()) {
                            return true;
                        }
                    }
                }
                else if(selection instanceof ITextSelection) {
                    ITextSelection tsel = (ITextSelection) selection;
                    IType type = getType(tsel);
                    if(type != null && type.isInterface()) {
                        return true;
                    }
                }
            }
            catch (JavaModelException e) {JDIDebugUIPlugin.log(e);}
        }
        return false;
    }
}
