/*****************************************************************************************
 * Copyright (c) 2004 Andrei Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrei Loskutov -
 * initial API and implementation
 ****************************************************************************************/
package de.loskutov.bco.views;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.AbstractToggleLinkingAction;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.loskutov.bco.BytecodeOutlinePlugin;
import de.loskutov.bco.asm.DecompiledClass;
import de.loskutov.bco.asm.DecompilerClassVisitor;
import de.loskutov.bco.compare.actions.ToggleASMifierModeAction;
import de.loskutov.bco.ui.EclipseUtils;
import de.loskutov.bco.ui.JdtUtils;

/**
 * This view shows decompiled java bytecode
 * @author Andrei
 */
public class BytecodeOutlineView extends ViewPart {

    protected boolean doLinkWithEditor;
    protected boolean selectedOnly;
    protected boolean showQualifiedNames;
    protected boolean isASMifierMode;
    protected boolean verifyCode;

    protected boolean bytecodeChanged;
    protected boolean selectionScopeChanged;
    protected boolean bufferIsDirty;

    private boolean isEnabled;
    private boolean isActive;
    private boolean isVisible;

    protected Composite stackComposite;
    protected StyledText textControl;
    protected Composite verifyControl;
    protected Table tableControl;
    protected StyledText stackControl;

    protected JavaEditor javaEditor;
    protected IJavaElement javaInput;
    protected IJavaElement lastChildElement;
    protected IJavaElement lastDecompiledElement;
    protected ITextSelection currentSelection;
    protected EditorListener editorListener;

    protected Action linkWithEditorAction;
    protected Action selectionChangedAction;
    protected Action showSelectedOnlyAction;
    protected Action setRawModeAction;
    protected Action toggleASMifierModeAction;
    protected Action toggleVerifierAction;

    protected Color errorColor;

    private DecompiledClass lastDecompiledResult;

    // ------------------------------------------------------------------------

    /**
     * The constructor.
     */
    public BytecodeOutlineView() {
        super();
    }

    // ------------------------------------------------------------------------

    /**
     * Is this view state changes depending on editor changes?
     * @return true if linked with editor
     */
    protected boolean isLinkedWithEditor() {
        return doLinkWithEditor;
    }

    /**
     * Are actions on toolbar active?
     * @return Returns the isEnabled.
     */
    private boolean isEnabled() {
        return isEnabled;
    }

    private void setEnabled(boolean on) {
        this.isEnabled = on;
        /*
         * if (textControl != null && !textControl.isDisposed()) {
         * textControl.setEnabled(on); }
         */
        if (tableControl != null && !tableControl.isDisposed()) {
            tableControl.setEnabled(on);
        }
        showSelectedOnlyAction.setEnabled(on);
        linkWithEditorAction.setEnabled(on);
        selectionChangedAction.setEnabled(on);
        setRawModeAction.setEnabled(on);
        toggleASMifierModeAction.setEnabled(on);
        toggleVerifierAction.setEnabled(on);
    }

    /**
     * Is this view monitoring workspace changes?
     * @return Returns the isActive.
     */
    private boolean isActive() {
        return isActive;
    }

    protected void toggleASMifierMode(boolean asmEnabled) {
        isASMifierMode = asmEnabled;
        selectionScopeChanged = true;
        setRawModeAction.setEnabled(!asmEnabled);
        refreshView();
    }

    /**
     * @param bufferIsDirty The bufferIsDirty to set.
     */
    private void setBufferIsDirty(boolean bufferIsDirty) {
        this.bufferIsDirty = bufferIsDirty;
    }

    /**
     * @param bytecodeChanged The bytecodeChanged to set.
     */
    private void setBytecodeChanged(boolean bytecodeChanged) {
        this.bytecodeChanged = bytecodeChanged;
    }

    private void setInput(JavaEditor editor) {
        javaEditor = null;
        javaInput = null;
        if (editor != null) {
            IJavaElement javaElem = EclipseUtils.getJavaInput(editor);
            if (javaElem == null) {
                return;
            }
            javaInput = javaElem;
            javaEditor = editor;
            updateSelection(EclipseUtils.getSelection(javaEditor
                .getSelectionProvider()));
            setBufferIsDirty(editor.isDirty());
        }
        setBytecodeChanged(true);
    }

    private void updateSelection(ITextSelection sel) {
        if (sel != null && sel.equals(currentSelection)) {
            return;
        }
        currentSelection = sel;
    }

    // ------------------------------------------------------------------------

    /**
     * @see org.eclipse.ui.IViewPart#init(org.eclipse.ui.IViewSite)
     */
    public void init(IViewSite site) {
        super.setSite(site);
        if (editorListener == null) {
            editorListener = new EditorListener(this);
            getSite().getWorkbenchWindow().getPartService().addPartListener(
                editorListener);
        }
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     * @param parent
     */
    public void createPartControl(Composite parent) {
        stackComposite = new Composite(parent, SWT.NONE);
        stackComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        stackComposite.setLayout(new StackLayout());

        textControl = new StyledText(stackComposite, SWT.H_SCROLL
            | SWT.V_SCROLL);
        textControl.setEditable(false);

        verifyControl = new SashForm(stackComposite, SWT.VERTICAL);
        tableControl = new Table(verifyControl, SWT.SINGLE | SWT.FULL_SELECTION);
        new TableColumn(tableControl, SWT.LEFT);
        new TableColumn(tableControl, SWT.LEFT);
        new TableColumn(tableControl, SWT.LEFT);
        tableControl.setLinesVisible(false);
        tableControl.setHeaderVisible(false);
        stackControl = new StyledText(verifyControl, SWT.H_SCROLL
            | SWT.V_SCROLL);
        stackControl.setEditable(false);
        ((SashForm) verifyControl).setWeights(new int[]{75, 25});

        ((StackLayout) stackComposite.getLayout()).topControl = textControl;

        errorColor = new Color(parent.getDisplay(), 255, 0, 0);

        textControl.addMouseListener(new MouseAdapter() {

            public void mouseDown(MouseEvent e) {
                if (isLinkedWithEditor()) {
                    selectionChangedAction.run();
                }
            }
        });

        tableControl.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (isLinkedWithEditor()) {
                    selectionChangedAction.run();
                }
            }
        });

        selectionChangedAction = new Action() {

            public void run() {
                Point selection = textControl.getSelection();
                setSelectionInJavaEditor(selection);
            }
        };

        linkWithEditorAction = new AbstractToggleLinkingAction() {

            public void run() {
                doLinkWithEditor = linkWithEditorAction.isChecked();
                if (doLinkWithEditor) {
                    showSelectedOnlyAction.setEnabled(true);
                    setRawModeAction.setEnabled(true);
                    toggleASMifierModeAction.setEnabled(true);
                    toggleVerifierAction.setEnabled(true);
                    checkOpenEditors(true);
                    // refreshView();
                } else {
                    showSelectedOnlyAction.setEnabled(false);
                    setRawModeAction.setEnabled(false);
                    toggleASMifierModeAction.setEnabled(false);
                    toggleVerifierAction.setEnabled(false);
                }
            }
        };
        // TODO get preference from store
        linkWithEditorAction.setText(BytecodeOutlinePlugin
            .getResourceString("BytecodeOutlineView.linkWithEditor_text"));
        linkWithEditorAction
            .setToolTipText(BytecodeOutlinePlugin
                .getResourceString("BytecodeOutlineView.linkWithEditorText_tooltip"));
        linkWithEditorAction.setChecked(true);
        doLinkWithEditor = true;

        showSelectedOnlyAction = new Action() {

            public void run() {
                selectedOnly = showSelectedOnlyAction.isChecked();
                selectionScopeChanged = true;
                refreshView();
            }
        };
        JavaPluginImages.setToolImageDescriptors(
            showSelectedOnlyAction, "segment_edit.gif");
        // TODO get preference from store
        showSelectedOnlyAction.setText(BytecodeOutlinePlugin
            .getResourceString("BytecodeOutlineView.showOnlySelection_text"));
        showSelectedOnlyAction
            .setToolTipText(BytecodeOutlinePlugin
                .getResourceString("BytecodeOutlineView.showOnlySelection_tooltip"));
        showSelectedOnlyAction.setChecked(true);
        selectedOnly = true;

        setRawModeAction = new Action() {

            public void run() {
                showQualifiedNames = setRawModeAction.isChecked();
                selectionScopeChanged = true;
                refreshView();
            }
        };
        setRawModeAction.setImageDescriptor(JavaPluginImages.DESC_OBJS_PACKAGE);
        // TODO get preference from store
        setRawModeAction.setText(BytecodeOutlinePlugin
            .getResourceString("BytecodeOutlineView.enableRawMode_text"));
        setRawModeAction.setToolTipText(BytecodeOutlinePlugin
            .getResourceString("BytecodeOutlineView.enableRawMode_tooltip"));
        setRawModeAction.setChecked(false);
        showQualifiedNames = false;

        toggleASMifierModeAction = new ToggleASMifierModeAction();
        toggleASMifierModeAction
            .addPropertyChangeListener(new IPropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent event) {
                    if (IAction.CHECKED.equals(event.getProperty())) {
                        toggleASMifierMode(Boolean.TRUE == event.getNewValue());
                    }
                }
            });
        // TODO get preference from store
        toggleASMifierModeAction.setChecked(false);
        isASMifierMode = false;

        toggleVerifierAction = new Action() {

            public void run() {
                verifyCode = toggleVerifierAction.isChecked();
                if (verifyCode) {
                    ((StackLayout) stackComposite.getLayout()).topControl = verifyControl;
                } else {
                    ((StackLayout) stackComposite.getLayout()).topControl = textControl;
                }
                stackComposite.layout();
                selectionScopeChanged = true;
                refreshView();
            }
        };
        // TODO get preference from store
        toggleVerifierAction.setImageDescriptor(AbstractUIPlugin
            .imageDescriptorFromPlugin(BytecodeOutlinePlugin.getDefault()
                .getBundle().getSymbolicName(), "icons/verify.gif"));
        toggleVerifierAction.setChecked(false);
        toggleVerifierAction.setText(BytecodeOutlinePlugin
            .getResourceString("BytecodeOutlineView.enableVerifier_text"));
        toggleVerifierAction.setToolTipText(BytecodeOutlinePlugin
            .getResourceString("BytecodeOutlineView.enableVerifier_tooltip"));
        verifyCode = false;

        IActionBars bars = getViewSite().getActionBars();

        IMenuManager mmanager = bars.getMenuManager();
        mmanager.add(linkWithEditorAction);
        mmanager.add(showSelectedOnlyAction);
        mmanager.add(setRawModeAction);
        mmanager.add(toggleASMifierModeAction);
        mmanager.add(toggleVerifierAction);

        IToolBarManager tmanager = bars.getToolBarManager();
        tmanager.add(linkWithEditorAction);
        tmanager.add(showSelectedOnlyAction);
        tmanager.add(setRawModeAction);
        tmanager.add(toggleASMifierModeAction);
        tmanager.add(toggleVerifierAction);

        setEnabled(false);
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    public void dispose() {
        deActivateView();
        if (editorListener != null) {
            getSite().getWorkbenchWindow().getPartService().removePartListener(
                editorListener);
            editorListener.dispose();
            editorListener = null;
        }
        if (textControl != null) {
            textControl.dispose();
            textControl = null;
        }
        if (verifyControl != null) {
            verifyControl.dispose();
            verifyControl = null;
            tableControl = null;
            stackControl = null;
        }
        if (errorColor != null) {
            errorColor.dispose();
            errorColor = null;
        }
        currentSelection = null;
        javaEditor = null;
        javaInput = null;
        lastChildElement = null;
        linkWithEditorAction = null;
        selectionChangedAction = null;
        showSelectedOnlyAction = null;
        setRawModeAction = null;
        toggleASMifierModeAction = null;
        toggleVerifierAction = null;
        lastDecompiledResult = null;
        super.dispose();
    }

    // -----------------------------------------------------------------------

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        if (!verifyCode) {
            if (textControl != null) {
                textControl.setFocus();
            }
        } else {
            if (tableControl != null) {
                tableControl.setFocus();
            }
        }
    }

    protected void handleBufferIsDirty(boolean isDirty) {
        if (!isLinkedWithEditor() || !isActive()) {
            return;
        }
        if (isDirty) {
            setBufferIsDirty(isDirty);
        } else {
            if (!bufferIsDirty) {
                // second time calling with same argument -
                // cause new bytecode should be written now
                setBytecodeChanged(true);
                refreshView();
            } else {
                // first time - set the flag only - cause
                // bytecode is not yet written
                setBufferIsDirty(false);
            }
        }
    }

    protected void handlePartHidden(IWorkbenchPart part) {
        if (!isLinkedWithEditor()) {
            return;
        }
        if (this == part) {
            isVisible = false;
            deActivateView();
        } else if (isActive() && (part instanceof IEditorPart)) {
            // check if at least one editor is open
            checkOpenEditors(false);
        }
    }

    protected void handlePartVisible(IWorkbenchPart part) {
        if (!isLinkedWithEditor()) {
            return;
        }
        if (this == part) {
            if (isVisible) {
                return;
            }
            isVisible = true;
            // check if java editor is already open
            IEditorPart activeEditor = EclipseUtils.getActiveEditor();
            if (!(activeEditor instanceof JavaEditor)) {
                // start monitoring again, even if current editor is not
                // supported - but we at front now
                activateView();
                return;
            }
            part = activeEditor;
            // continue with setting input
        }
        if (isVisible && part instanceof JavaEditor) {
            if (isActive() && part == javaEditor) {
                return;
            }
            activateView();
            setEnabled(true);
            setInput((JavaEditor) part);
            refreshView();
        } else if (part instanceof IEditorPart) {
            if (isActive()) {
                deActivateView();
            }
        }
    }

    protected void handleSelectionChanged(IWorkbenchPart part,
        ISelection selection) {
        if (!isLinkedWithEditor() || !isActive()
            || !(part instanceof IEditorPart)) {
            return;
        }
        if (!(part instanceof JavaEditor)) {
            deActivateView();
            return;
        }
        if (!isEnabled()) {
            setEnabled(true);
        }
        if (part != javaEditor) {
            setInput((JavaEditor) part);
        } else {
            updateSelection((ITextSelection) selection);
        }
        refreshView();
    }

    /**
     * Does nothing if view is already active
     */
    private void activateView() {
        if (isActive()) {
            return;
        }
        isActive = true;
        getSite().getWorkbenchWindow().getSelectionService()
            .addPostSelectionListener(editorListener);
        FileBuffers.getTextFileBufferManager().addFileBufferListener(
            editorListener);
    }

    /**
     * Does nothing if view is already deactivated
     */
    private void deActivateView() {
        if (!isActive()) {
            return;
        }
        setEnabled(false);
        if (editorListener != null) {
            ISelectionService service = getSite().getWorkbenchWindow()
                .getSelectionService();
            service.removePostSelectionListener(editorListener);
            FileBuffers.getTextFileBufferManager().removeFileBufferListener(
                editorListener);

        }
        if (textControl != null && !textControl.isDisposed()) {
            textControl.setText("");
        }
        if (tableControl != null && !tableControl.isDisposed()) {
            setItems(null);
        }
        currentSelection = null;
        lastDecompiledResult = null;
        lastDecompiledElement = null;
        javaEditor = null;
        javaInput = null;
        lastChildElement = null;
        setBufferIsDirty(false);
        isActive = false;
    }

    protected void refreshView() {
        if (!isActive() || !isLinkedWithEditor()) {
            return;
        }
        boolean scopeChanged = selectionScopeChanged;
        selectionScopeChanged = false;
        if (javaInput == null || currentSelection == null) {
            deActivateView();
            return;
        }
        IJavaElement childEl = null;
        try {
            childEl = JdtUtils.getElementAtOffset(javaInput, currentSelection);
            if (childEl != null) {
                switch (childEl.getElementType()) {
                    case IJavaElement.METHOD :
                    case IJavaElement.FIELD :
                    case IJavaElement.INITIALIZER :
                        break;
                    case IJavaElement.LOCAL_VARIABLE :
                        childEl = childEl.getAncestor(IJavaElement.METHOD);
                        break;
                    default :
                        childEl = null;
                        break;
                }
            }
        } catch (JavaModelException e) {
            BytecodeOutlinePlugin.error(null, e);
        }
        if (isJavaStructureChanged(childEl) || scopeChanged) {
            bytecodeChanged = false;
            lastChildElement = childEl;
            DecompiledClass result = decompileBytecode(childEl);
            if (result != null) {
                if (!verifyCode) {
                    textControl.setText(result.getText());
                } else {
                    setItems(result.getTextTable());
                    List errors = result.getErrorLines();
                    for (int i = 0; i < errors.size(); ++i) {
                        int l = ((Integer) errors.get(i)).intValue();
                        tableControl.getItem(l).setForeground(errorColor);
                    }
                }
            } else {
                if (!verifyCode) {
                    textControl.setText("");
                } else {
                    setItems(null);
                }
            }
            lastDecompiledResult = result;
        }
        setSelectionInBytecodeView();
    }

    private void setSelectionInBytecodeView() {
        if (lastDecompiledResult == null) {
            return;
        }

        int sourceLine = currentSelection.getStartLine() + 1;
        int decompiledLine = lastDecompiledResult.getDecompiledLine(sourceLine);

        if (decompiledLine > 0) {
            try {
                if (verifyCode) {
                    tableControl.select(decompiledLine);
                    String frame = lastDecompiledResult
                        .getFrame(decompiledLine);
                    if (frame != null) {
                        stackControl.setText(frame);
                    }
                } else {
                    int offsetAtLine = textControl
                        .getOffsetAtLine(decompiledLine);
                    int offsetEnd = textControl.getText().indexOf(
                        '\n', offsetAtLine);
                    textControl.setSelection(offsetAtLine, offsetEnd);
                }
            } catch (IllegalArgumentException e) {
                BytecodeOutlinePlugin.error(null, e);
            }
        }
    }

    protected void setSelectionInJavaEditor(Point selection) {
        if (javaEditor != null && javaEditor.getViewer() == null) {
            // editor was closed - we should clean the reference
            javaEditor = null;
            javaInput = null;
        }
        if (javaEditor == null || lastDecompiledResult == null) {
            deActivateView();
            return;
        }

        int decompiledLine;
        if (verifyCode) {
            decompiledLine = tableControl.getSelectionIndex();
        } else {
            decompiledLine = textControl.getLineAtOffset(selection.x);
        }
        int sourceLine = lastDecompiledResult.getSourceLine(decompiledLine);

        try {
            if (sourceLine > 0) {
                IRegion lineInformation1 = javaEditor.getViewer().getDocument()
                    .getLineInformation(sourceLine - 1);

                EclipseUtils.selectInEditor(javaEditor, lineInformation1
                    .getOffset(), lineInformation1.getLength());
            }
            if (verifyCode) {
                String frame = lastDecompiledResult.getFrame(decompiledLine);
                if (frame != null) {
                    stackControl.setText(frame);
                }
            }
        } catch (Exception e) {
            BytecodeOutlinePlugin.logError(e);
        }
    }

    // ------------------------------------------------------------------------

    /**
     * check if at least one java editor is open - if not, deactivate me
     * @param checkNewSelection
     */
    protected void checkOpenEditors(boolean checkNewSelection) {
        IEditorReference[] editorReferences = getSite().getPage()
            .getEditorReferences();
        if (editorReferences == null || editorReferences.length == 0) {
            deActivateView();
        } else {
            if (checkNewSelection) {
                IEditorPart activeEditor = EclipseUtils.getActiveEditor();
                if (activeEditor instanceof JavaEditor) {
                    ITextSelection selection = EclipseUtils
                        .getSelection(((JavaEditor) activeEditor)
                            .getSelectionProvider());
                    handleSelectionChanged(activeEditor, selection);
                    return;
                }
            }
            for (int i = 0; i < editorReferences.length; i++) {
                IEditorPart editor = editorReferences[i].getEditor(false);
                if (editor instanceof JavaEditor) {
                    return;
                }
            }
            // here are all editors checked and no one is java editor
            deActivateView();
        }
    }

    /**
     * @param childEl
     * @return true if either bytecode was rewritten or selection was changed
     */
    private boolean isJavaStructureChanged(IJavaElement childEl) {
        if (bytecodeChanged || lastDecompiledElement == null) {
            return true;
        }
        if (lastChildElement == null && childEl == null) {
            // no selected child - we stay by entire class bytecode
            return false;
        } else if (lastChildElement == null
            || !lastChildElement.equals(childEl)) {
            return true;
        }
        return true;
    }

    /**
     * @param childEl
     * @return return null if type is not known or bytecode is not written
     * or cannot be found
     */
    private DecompiledClass decompileBytecode(IJavaElement childEl) {
        // check here for inner classes too
        IJavaElement type = JdtUtils.getEnclosingType(childEl);
        if (type == null) {
            type = javaInput;
        }
        InputStream is = JdtUtils.createInputStream(type);
        lastDecompiledElement = type;
        if (is == null) {
            return null;
        }

        ClassLoader cl = null;
        if (verifyCode) {
            cl = getClassLoader(type);
        }
        
        String fieldName = null;
        String methodName = null;

        /*
         * find out, which name we should use for selected element
         */
        if (selectedOnly && childEl != null) {
            if (childEl.getElementType() == IJavaElement.FIELD) {
                fieldName = childEl.getElementName();
            } else if (childEl.getElementType() == IJavaElement.INITIALIZER) {
                IInitializer ini = (IInitializer) childEl;
                try {
                    if (Flags.isStatic(ini.getFlags())) {
                        methodName = "<clinit>()V";
                    } else {
                        methodName = "<init>()";
                    }
                } catch (JavaModelException e) {
                    // this is compilation problem - don't show message
                    BytecodeOutlinePlugin.logError(e);
                }
            } else if (childEl.getElementType() == IJavaElement.METHOD) {
                IMethod iMethod = (IMethod) childEl;
                try {
                    methodName = JdtUtils.createMethodSignature(iMethod);
                } catch (JavaModelException e) {
                    // this is compilation problem - don't show message
                    BytecodeOutlinePlugin.logError(e);
                }
            }
        }
        
        DecompiledClass decompiledClass = null;
        try {
            decompiledClass = DecompilerClassVisitor.getDecompiledClass(
                is, fieldName, methodName, showQualifiedNames, isASMifierMode,
                verifyCode, cl);
        } catch (IOException e) {
            try {
                // check if compilation unit is ok - then this is user problem
                if (type != null && type.isStructureKnown()) {
                    BytecodeOutlinePlugin.error(null, e);
                }
            } catch (JavaModelException e1) {
                // this is compilation problem - don't show message
                BytecodeOutlinePlugin.logError(e);
                BytecodeOutlinePlugin.logError(e1);
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                BytecodeOutlinePlugin.logError(e);
            }
        }
        return decompiledClass;
    }

    /**
     * @param type
     * @return
     */
    private ClassLoader getClassLoader(IJavaElement type) {
        ClassLoader cl;

        IJavaProject javaProject = type.getJavaProject();
        IPath projectPath = javaProject.getProject().getLocation();
        IClasspathEntry[] paths = null;
        IPath defaultOutputLocation = null;
        try {
            paths = javaProject.getResolvedClasspath(true);
            defaultOutputLocation = javaProject.getOutputLocation();
        } catch (JavaModelException e) {
            // don't show message to user
            BytecodeOutlinePlugin.logError(e);
        }
        if (paths == null) {
            return getClass().getClassLoader();
        }
        List urls = new ArrayList();
        for (int i = 0; i < paths.length; ++i) {
            IClasspathEntry cpEntry = paths[i];
            IPath p = null;
            if (cpEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                // filter out source container - there are unused for class search -
                // add bytecode output location instead
                p = cpEntry.getOutputLocation();
                if (p == null) {
                    // default output used:
                    p = defaultOutputLocation;
                }
            } else {
                p = cpEntry.getPath();
            }

            if (p == null) {
                continue;
            }
            if (!p.toFile().exists()) {
                // removeFirstSegments: remove project from relative path
                p = projectPath.append(p.removeFirstSegments(1));
                if (!p.toFile().exists()) {
                    continue;
                }
            }
            try {
                urls.add(p.toFile().toURL());
            } catch (MalformedURLException e) {
                // don't show message to user
                BytecodeOutlinePlugin.logError(e);
            }
        }
        if (urls.isEmpty()) {
            cl = getClass().getClassLoader();
        } else {
            cl = new URLClassLoader((URL[]) urls.toArray(new URL[urls.size()]));
        }
        return cl;
    }

    private void setItems(String[][] items) {
        tableControl.removeAll();
        if (items != null) {
            for (int i = 0; i < items.length; ++i) {
                TableItem item = new TableItem(tableControl, SWT.NONE);
                for (int j = 0; j < items[i].length; ++j) {
                    String s = items[i][j];
                    if (s.endsWith("\n")) {
                        s = s.substring(0, s.length() - 1);
                    }
                    item.setText(j, s);
                }
            }
        }
        tableControl.getColumn(0).pack();
        tableControl.getColumn(1).pack();
        tableControl.getColumn(2).pack();
    }
}