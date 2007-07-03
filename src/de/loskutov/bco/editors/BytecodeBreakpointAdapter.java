package de.loskutov.bco.editors;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaFieldVariable;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.JavaDebugUtils;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.debug.ui.actions.BreakpointFieldLocator;
import org.eclipse.jdt.internal.debug.ui.actions.BreakpointLocationVerifierJob;
import org.eclipse.jdt.internal.debug.ui.actions.BreakpointMethodLocator;
import org.eclipse.jdt.internal.debug.ui.actions.ToggleBreakpointAdapter;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Extend ToggleBreakpointAdapter to allow us map source code lines to the bytecode lines
 * TODO implement the mapping :)
 * @author Andrei
 *
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
        Job job = new Job("Toggle Method Breakpoints") { //$NON-NLS-1$
            protected IStatus run(IProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                if(isInterface(finalSelection)) {
                    report(ActionMessages.ToggleBreakpointAdapter_7, part);
                    return Status.OK_STATUS;
                }
                try {
                    report(null, part);
                    ISelection selection = finalSelection;
                    selection = translateToMembers(part, selection);
                    ITextEditor textEditor = getTextEditor(part);
                    if (textEditor != null && selection instanceof ITextSelection) {
                        ITextSelection textSelection = (ITextSelection) selection;
                        CompilationUnit compilationUnit = parseCompilationUnit(textEditor);
                        if (compilationUnit != null) {
                            BreakpointMethodLocator locator = new BreakpointMethodLocator(textSelection.getOffset());
                            compilationUnit.accept(locator);
                            String methodName = locator.getMethodName();
                            if (methodName == null) {
                                report("Method breakpoints can only be added to concrete methods.", part);
                                return Status.OK_STATUS;
                            }
                            String typeName = locator.getTypeName();
                            String methodSignature = locator.getMethodSignature();
                            if (methodSignature == null) {
                                report(ActionMessages.ManageMethodBreakpointActionDelegate_methodNonAvailable, part);
                                return Status.OK_STATUS;
                            }
                            // check if this method breakpoint already
                            // exist. If yes, remove it, else create one
                            IJavaMethodBreakpoint existing = getMethodBreakpoint(typeName, methodName, methodSignature);
                            if (existing == null) {
                                createMethodBreakpoint(getResource((IEditorPart) part), typeName, methodName, methodSignature, true, false, false, -1, -1, -1, 0, true, new HashMap(10));
                            } else {
                                removeBreakpoint(existing, true);
                            }
                        }
                    } else if (selection instanceof IStructuredSelection) {
                        IMethod[] members = getMethods((IStructuredSelection) selection);
                        if (members.length == 0) {
                            report(ActionMessages.ToggleBreakpointAdapter_9, part);
                            return Status.OK_STATUS;
                        }
                        for (int i = 0, length = members.length; i < length; i++) {
                            IMethod method = members[i];
                            IJavaBreakpoint breakpoint = getMethodBreakpoint(method);
                            if (breakpoint == null) {
                                // add breakpoint
                                int start = -1;
                                int end = -1;
                                ISourceRange range = method.getNameRange();
                                if (range != null) {
                                    start = range.getOffset();
                                    end = start + range.getLength();
                                }
                                Map attributes = new HashMap(10);
                                BreakpointUtils.addJavaBreakpointAttributes(attributes, method);
                                IType type = method.getDeclaringType();
                                String methodSignature = method.getSignature();
                                String methodName = method.getElementName();
                                if (method.isConstructor()) {
                                    methodName = "<init>"; //$NON-NLS-1$
                                    if (type.isEnum()) {
                                        methodSignature = "(Ljava.lang.String;I" + methodSignature.substring(1); //$NON-NLS-1$
                                    }
                                }
                                if (!type.isBinary()) {
                                    // resolve the type names
                                    methodSignature = resolveMethodSignature(type, methodSignature);
                                    if (methodSignature == null) {
                                        report(ActionMessages.ManageMethodBreakpointActionDelegate_methodNonAvailable, part);
                                        return Status.OK_STATUS;
                                    }
                                }
                                createMethodBreakpoint(BreakpointUtils.getBreakpointResource(method), type.getFullyQualifiedName(), methodName, methodSignature, true, false, false, -1, start, end, 0, true, attributes);
                            } else {
                                // remove breakpoint
                                removeBreakpoint(breakpoint, true);
                            }
                        }
                    }
                } catch (CoreException e) {
                    return e.getStatus();
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();

    }

    public void toggleWatchpoints(final IWorkbenchPart part, final ISelection finalSelection) {
        Job job = new Job("Toggle Watchpoints") { //$NON-NLS-1$
            protected IStatus run(IProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                if(isInterface(finalSelection)) {
                    report(ActionMessages.ToggleBreakpointAdapter_5, part);
                    return Status.OK_STATUS;
                }
                try {
                    report(null, part);
                    ISelection selection = finalSelection;
                    selection = translateToMembers(part, selection);
                    ITextEditor textEditor = getTextEditor(part);
                    boolean allowed = false;
                    if (textEditor != null && selection instanceof ITextSelection) {
                        ITextSelection textSelection = (ITextSelection) selection;
                        CompilationUnit compilationUnit = parseCompilationUnit(textEditor);
                        if (compilationUnit != null) {
                            BreakpointFieldLocator locator = new BreakpointFieldLocator(textSelection.getOffset());
                            compilationUnit.accept(locator);
                            String fieldName = locator.getFieldName();
                            if (fieldName == null) {
                                report("Watchpoints can only be added to field members.", part);
                                return Status.OK_STATUS;
                            }
                            int idx = fieldName.indexOf("final"); //$NON-NLS-1$
                            if(!(idx > -1) & !(fieldName.indexOf("static") > -1 & idx > -1)) { //$NON-NLS-1$
                                allowed = true;
                            }
                            String typeName = locator.getTypeName();
                            // check if the watchpoint already exists. If yes,
                            // remove it, else create one
                            IJavaWatchpoint existing = getWatchpoint(typeName, fieldName);
                            if (existing == null) {
                                if(!allowed) {
                                    report("You cannot create create watchpoints on final or static final members.", part);
                                    return Status.OK_STATUS;
                                }
                                createWatchpoint(getResource((IEditorPart) part), typeName, fieldName, -1, -1, -1, 0, true, new HashMap(10));
                            } else {
                                removeBreakpoint(existing, true);
                            }
                        }
                    } else if (selection instanceof IStructuredSelection) {
                        List fields = getFields((IStructuredSelection) selection);
                        if (fields.isEmpty()) {
                            report(ActionMessages.ToggleBreakpointAdapter_10, part);
                            return Status.OK_STATUS;
                        }
                        Iterator theFields = fields.iterator();
                        while (theFields.hasNext()) {
                            Object element = theFields.next();
                            IField javaField = null;
                            IJavaFieldVariable var = null;
                            String typeName = null;
                            String fieldName = null;
                            if (element instanceof IField) {
                                javaField = (IField) element;
                                typeName = javaField.getDeclaringType().getFullyQualifiedName();
                                fieldName = javaField.getElementName();
                                int f = javaField.getFlags();
                                boolean fin = Flags.isFinal(f);
                                allowed = !fin && !(Flags.isStatic(f) && fin);
                            } else if (element instanceof IJavaFieldVariable) {
                                var = (IJavaFieldVariable) element;
                                typeName = var.getDeclaringType().getName();
                                fieldName = var.getName();
                                allowed = !(var.isFinal() || var.isStatic());
                            }
                            IJavaBreakpoint breakpoint = getWatchpoint(typeName, fieldName);
                            if (breakpoint == null) {
                                if(!allowed) {
                                    report("You cannot create create watchpoints on final or static final members.", part);
                                    return Status.OK_STATUS;
                                }
                                IResource resource = null;
                                int start = -1;
                                int end = -1;
                                Map attributes = new HashMap(10);
                                if (javaField == null) {
                                    if(var != null) {
                                        Object object = JavaDebugUtils.resolveSourceElement(var.getJavaType(), var.getLaunch());
                                        if (object instanceof IAdaptable) {
                                            IAdaptable adaptable = (IAdaptable) object;
                                            resource = (IResource) adaptable.getAdapter(IResource.class);
                                        }
                                    }
                                    if (resource == null) {
                                        resource = ResourcesPlugin.getWorkspace().getRoot();
                                    }
                                } else {
                                    IType type = javaField.getDeclaringType();
                                    ISourceRange range = javaField.getNameRange();
                                    if (range != null) {
                                        start = range.getOffset();
                                        end = start + range.getLength();
                                    }
                                    BreakpointUtils.addJavaBreakpointAttributes(attributes, javaField);
                                    resource = BreakpointUtils.getBreakpointResource(type);
                                }
                                createWatchpoint(resource, typeName, fieldName, -1, start, end, 0, true, attributes);
                            } else {
                                // remove breakpoint
                                removeBreakpoint(breakpoint, true);
                            }
                        }
                    }
                } catch (CoreException e) {
                    return e.getStatus();
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    /**
     * Toggles a line breakpoint. This is also the method called by the keybinding for creating breakpoints
     * @param part the currently active workbench part
     * @param selection the current selection
     * @param bestMatch if we should make a best match or not
     */
    public void toggleLineBreakpoints(final IWorkbenchPart part, final ISelection selection, final boolean bestMatch) {
        Job job = new Job("Toggle Line Breakpoint") { //$NON-NLS-1$
            protected IStatus run(IProgressMonitor monitor) {
                if(isInterface(selection)) {
                    report(ActionMessages.ToggleBreakpointAdapter_6, part);
                    return Status.OK_STATUS;
                }
                ITextEditor editor = getTextEditor(part);
                if (editor != null && selection instanceof ITextSelection) {
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    report(null, part);
                    ITextSelection textSelection = (ITextSelection) selection;
                    IType type = getType(textSelection);
                    int lineNumber = textSelection.getStartLine() + 1;
                    int offset = textSelection.getOffset();
                    try {
                        IEditorInput editorInput = editor.getEditorInput();
                        IDocumentProvider documentProvider = editor.getDocumentProvider();
                        if (documentProvider == null) {
                            return Status.CANCEL_STATUS;
                        }
                        IDocument document = documentProvider.getDocument(editorInput);
                        if (type == null) {
                            IClassFile classFile = (IClassFile) editorInput.getAdapter(IClassFile.class);
                            if (classFile != null) {
                                type = classFile.getType();
                                // bug 34856 - if this is an inner type, ensure
                                // the breakpoint is not
                                // being added to the outer type
                                if (type.getDeclaringType() != null) {
                                    ISourceRange sourceRange = type.getSourceRange();
                                    int start = sourceRange.getOffset();
                                    int end = start + sourceRange.getLength();
                                    if (offset < start || offset > end) {
                                        // not in the inner type
                                        IStatusLineManager statusLine = editor.getEditorSite().getActionBars().getStatusLineManager();
                                        statusLine.setErrorMessage(MessageFormat.format("Breakpoints can only be created within the type associated with the editor: {0}.", new String[] { type.getTypeQualifiedName() }));
                                        Display.getCurrent().beep();
                                        return Status.OK_STATUS;
                                    }
                                }
                            }
                        }
                        String typeName = null;
                        IResource resource = null;
                        Map attributes = new HashMap(10);
                        if (type == null) {
                            resource = getResource(editor);
                            CompilationUnit unit = parseCompilationUnit(editor);
                            Iterator types = unit.types().iterator();
                            // TODO unreachable API
//                            while (types.hasNext()) {
//                                TypeDeclaration declaration = (TypeDeclaration) types.next();
//                                int begin = declaration.getStartPosition();
//                                int end = begin + declaration.getLength();
//                                if (offset >= begin && offset <= end && !declaration.isInterface()) {
//                                    typeName = ValidBreakpointLocationLocator.computeTypeName(declaration);
//                                    break;
//                                }
//                            }
                        } else {
                            typeName = type.getFullyQualifiedName();
                            int index = typeName.indexOf('$');
                            if (index >= 0) {
                                typeName = typeName.substring(0, index);
                            }
                            resource = BreakpointUtils.getBreakpointResource(type);
                            try {
                                IRegion line = document.getLineInformation(lineNumber - 1);
                                int start = line.getOffset();
                                int end = start + line.getLength() - 1;
                                BreakpointUtils.addJavaBreakpointAttributesWithMemberDetails(attributes, type, start, end);
                            } catch (BadLocationException ble) {
                                JDIDebugUIPlugin.log(ble);
                            }
                        }
                        if (typeName != null && resource != null) {
                            IJavaLineBreakpoint existingBreakpoint = JDIDebugModel.lineBreakpointExists(resource, typeName, lineNumber);
                            if (existingBreakpoint != null) {
                                removeBreakpoint(existingBreakpoint, true);
                                return Status.OK_STATUS;
                            }
                            createLineBreakpoint(resource, typeName, lineNumber, -1, -1, 0, true, attributes, document, bestMatch, type, editor);
                        }
                    } catch (CoreException ce) {
                        return ce.getStatus();
                    }
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
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
