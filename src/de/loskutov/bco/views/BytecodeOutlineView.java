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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.AbstractToggleLinkingAction;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.console.actions.TextViewerAction;
import org.eclipse.ui.internal.layout.TrimLayout;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

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
    // orientations
    static final int VIEW_ORIENTATION_VERTICAL = 0;
    static final int VIEW_ORIENTATION_HORIZONTAL = 1;
    static final int VIEW_ORIENTATION_AUTOMATIC = 2;

    /**
     * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
     * <code>VIEW_ORIENTATION_VERTICAL</code>, or <code>VIEW_ORIENTATION_AUTOMATIC</code>.
     */
    int orientation = VIEW_ORIENTATION_AUTOMATIC;
    /**
     * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
     * <code>VIEW_ORIENTATION_VERTICAL</code>.
     */
    private int currentOrientation;

    private ToggleOrientationAction[] toggleOrientationActions;
    private Composite parent;

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
    protected TextViewer textViewer;
    protected SashForm verifyControl;
    protected SashForm stackAndLvt;
    protected Table tableControl;
    // protected StyledText stackControl;
    // protected StyledText lvtControl;
    protected Table stackTable;
    protected Table lvtTable;

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
    protected StatusLineManager statusLineManager;

    protected Color errorColor;

    private DecompiledClass lastDecompiledResult;

    protected Map globalActions = new HashMap();
    protected List selectionActions = new ArrayList();
    private MenuManager contextMenuManager;
    /** global class info, without current selection status */
    protected String currentStatusMessage;
    protected boolean hasAnalyzerError;

    private static final String NLS_PREFIX = "BytecodeOutlineView.";

    // updates the find replace action if the document length is > 0
    private ITextListener textListener = new ITextListener() {
        public void textChanged(TextEvent event) {
            IUpdate findReplace = (IUpdate) globalActions
                .get(ActionFactory.FIND.getId());
            if (findReplace != null) {
                findReplace.update();
            }
        }
    };

    // see org.eclipse.ui.console.TextConsolePage for the reason to do this ;)
    private ISelectionChangedListener selectionChangedListener =
        new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
            updateSelectionDependentActions();
        }
    };
    private Control statusControl;

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
        if (tableControl != null && !tableControl.isDisposed()) {
            tableControl.setEnabled(on);
        }
        if(stackTable != null && !stackTable.isDisposed()){
            stackTable.setEnabled(on);
        }
        if(lvtTable != null && !lvtTable.isDisposed()){
            lvtTable.setEnabled(on);
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
        lastDecompiledResult = null;
        lastDecompiledElement = null;
        if (editor != null) {
            IJavaElement javaElem = EclipseUtils.getJavaInput(editor);
            if (javaElem == null) {
                return;
            }
            javaInput = javaElem;
            javaEditor = editor;

            checkVerifyAction(javaInput);

            updateSelection(EclipseUtils.getSelection(javaEditor
                .getSelectionProvider()));
            setBufferIsDirty(editor.isDirty());

        }
        setBytecodeChanged(true);
    }

    /**
     * @param javaElement
     * @return true if verify mode could be enabled
     */
    private boolean checkVerifyAction(IJavaElement javaElement) {
        boolean aoi = JdtUtils.isAbstractOrInterface(javaElement);
        // deactivate, but only if not in verify mode
        if(!toggleVerifierAction.isChecked()){
            toggleVerifierAction.setEnabled(!aoi);
        }
        return !aoi;
    }

    private boolean updateSelection(ITextSelection sel) {
        if (sel != null
            && (sel.equals(currentSelection)
                || (currentSelection != null &&
                 sel.getStartLine() == currentSelection
                .getStartLine() && sel.getEndLine() == currentSelection
                .getEndLine()))) {

            /* getStartLine/getEndLine is probably not sensitive enough - but
             * in case of java classes/methods which fits in one selection but
             * not in the other, then I think we can ignore them here - this is
             * not the 99% of use cases.
             */
            return false;
        }

        currentSelection = sel;
        return true;
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
     * @param parent1
     */
    public void createPartControl(Composite parent1) {
        this.parent = parent1;
        parent1.addControlListener(new ControlListener() {
            public void controlMoved(ControlEvent e) {
                //
            }
            public void controlResized(ControlEvent e) {
                computeOrientation();
            }
        });

        Composite mainComposite = new Composite(parent1, SWT.NONE);
        TrimLayout tLayout = new TrimLayout();
        mainComposite.setLayout(tLayout);

        stackComposite = new Composite(mainComposite, SWT.NONE);
        tLayout.setCenterControl(stackComposite);
        stackComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        stackComposite.setLayout(new StackLayout());

        statusLineManager = new StatusLineManager();
        statusControl = statusLineManager.createControl(mainComposite, SWT.NONE);
        tLayout.addTrim(statusControl, SWT.BOTTOM);
        //statusLineManager.setErrorMessage("hallo!");

// init text viewer ans some related actions -----------------------------------
// TODO make init code clear

        textViewer = new TextViewer(stackComposite, SWT.H_SCROLL  | SWT.V_SCROLL);
        textViewer.setEditable(false);
        //textViewer.setHoverControlCreator(null)

        textControl = textViewer.getTextWidget();
        IDocument document= new Document("");
        textViewer.setDocument(document);

        textViewer.getSelectionProvider().addSelectionChangedListener(
            selectionChangedListener);
        textViewer.addTextListener(textListener);

        createActions();

        /*
         * create/register context menu on text control
         */
        String id = "de.loskutov.bco.views.BytecodeOutlineView#ContextMenu"; //$NON-NLS-1$
        contextMenuManager= new MenuManager("#ContextMenu", id);  //$NON-NLS-1$//$NON-NLS-2$
        contextMenuManager.setRemoveAllWhenShown(true);
        contextMenuManager.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager m) {
                contextMenuAboutToShow(m);
            }
        });
        Menu menu = contextMenuManager.createContextMenu(textControl);
        textControl.setMenu(menu);

        getSite().registerContextMenu(id, contextMenuManager, textViewer); //$NON-NLS-1$
        getSite().setSelectionProvider(textViewer);

//----------------------------------------

        verifyControl = new SashForm(stackComposite, SWT.VERTICAL);

        tableControl = new Table(verifyControl, SWT.SINGLE | SWT.FULL_SELECTION);
        new TableColumn(tableControl, SWT.LEFT).setText( BytecodeOutlinePlugin.getResourceString(NLS_PREFIX + "lvt.header"));
        new TableColumn(tableControl, SWT.LEFT).setText( BytecodeOutlinePlugin.getResourceString(NLS_PREFIX + "stack.header"));
        new TableColumn(tableControl, SWT.LEFT);
        tableControl.setLinesVisible(false);
        tableControl.setHeaderVisible(true);


        stackAndLvt = new SashForm(verifyControl, SWT.HORIZONTAL);

        lvtTable = new Table( stackAndLvt, SWT.SINGLE | SWT.FULL_SELECTION);
        lvtTable.setLinesVisible(false);
        lvtTable.setHeaderVisible(true);

        new TableColumn( lvtTable, SWT.LEFT).setText( "#");
        new TableColumn( lvtTable, SWT.LEFT).setText( "Var Type");
        new TableColumn( lvtTable, SWT.LEFT).setText( "Name");

        stackTable = new Table( stackAndLvt, SWT.SINGLE | SWT.FULL_SELECTION);
        stackTable.setLinesVisible(false);
        stackTable.setHeaderVisible(true);
        new TableColumn( stackTable, SWT.LEFT).setText( "#");
        new TableColumn( stackTable, SWT.LEFT).setText( "Stack Type");

        stackAndLvt.setWeights(new int[]{50, 50});


        verifyControl.setWeights(new int[]{75, 25});

        ((StackLayout) stackComposite.getLayout()).topControl = textControl;

        errorColor = new Color(parent1.getDisplay(), 255, 0, 0);

        textControl.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent e) {
                if (isLinkedWithEditor()) {
                    selectionChangedAction.run();
                }
            }
        });

        textControl.addKeyListener(new KeyListener(){
            public void keyPressed(KeyEvent e) {
                // ignored
            }
            public void keyReleased(KeyEvent e) {
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

        linkWithEditorAction.setText(BytecodeOutlinePlugin
            .getResourceString(NLS_PREFIX + "linkWithEditor.label"));
        linkWithEditorAction
            .setToolTipText(BytecodeOutlinePlugin
                .getResourceString(NLS_PREFIX + "linkWithEditorText.tooltip"));

        // TODO get preference from store
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
        showSelectedOnlyAction.setText(BytecodeOutlinePlugin
            .getResourceString(NLS_PREFIX + "showOnlySelection.label"));
        showSelectedOnlyAction
            .setToolTipText(BytecodeOutlinePlugin
                .getResourceString(NLS_PREFIX + "showOnlySelection.tooltip"));

        // TODO get preference from store
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
        setRawModeAction.setText(BytecodeOutlinePlugin
            .getResourceString(NLS_PREFIX + "enableRawMode.label"));
        setRawModeAction.setToolTipText(BytecodeOutlinePlugin
            .getResourceString(NLS_PREFIX + "enableRawMode.tooltip"));
        // TODO get preference from store
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

        toggleVerifierAction.setImageDescriptor(AbstractUIPlugin
            .imageDescriptorFromPlugin(BytecodeOutlinePlugin.getDefault()
                .getBundle().getSymbolicName(), "icons/verify.gif"));

        // TODO get preference from store
        toggleVerifierAction.setChecked(false);
        toggleVerifierAction.setText(BytecodeOutlinePlugin
            .getResourceString(NLS_PREFIX + "enableVerifier.label"));
        toggleVerifierAction.setToolTipText(BytecodeOutlinePlugin
            .getResourceString(NLS_PREFIX + "enableVerifier.tooltip"));
        verifyCode = false;

        IActionBars bars = getViewSite().getActionBars();

        IMenuManager mmanager = bars.getMenuManager();
        mmanager.add(linkWithEditorAction);
        mmanager.add(showSelectedOnlyAction);
        mmanager.add(setRawModeAction);
        mmanager.add(toggleASMifierModeAction);
        mmanager.add(toggleVerifierAction);

        mmanager.add( new Separator());

        toggleOrientationActions = new ToggleOrientationAction[] {
            new ToggleOrientationAction(this, VIEW_ORIENTATION_VERTICAL),
            new ToggleOrientationAction(this, VIEW_ORIENTATION_HORIZONTAL),
            new ToggleOrientationAction(this, VIEW_ORIENTATION_AUTOMATIC)};
        for (int i = 0; i < toggleOrientationActions.length; ++i) {
            mmanager.add(toggleOrientationActions[i]);
        }

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

        if (contextMenuManager != null) {
            contextMenuManager.dispose();
        }

        selectionActions.clear();
        globalActions.clear();

        textViewer.getSelectionProvider().removeSelectionChangedListener(
            selectionChangedListener);
        textViewer.removeTextListener(textListener);
        textViewer = null;

        if (textControl != null) {
            textControl.dispose();
            textControl = null;
        }
        if (verifyControl != null) {
            verifyControl.dispose();
            verifyControl = null;
            tableControl = null;
            stackTable = null;
            lvtTable = null;
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


    /**
     * Fill the context menu
     *
     * @param menuManager menu
     */
    protected void contextMenuAboutToShow(IMenuManager menuManager) {
        IDocument doc= textViewer.getDocument();
        if (doc == null) {
            return;
        }

        menuManager.add((IAction)globalActions.get(ActionFactory.COPY.getId()));
        menuManager.add((IAction)globalActions.get(ActionFactory.SELECT_ALL.getId()));

        menuManager.add(new Separator("FIND")); //$NON-NLS-1$
        menuManager.add((IAction)globalActions.get(ActionFactory.FIND.getId()));

        menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    // -----------------------------------------------------------------------

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        if (!verifyCode) {
            if (textViewer != null) {
                textViewer.getTextWidget().setFocus();
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
                // after refresh it is possible that java type is disappear
                // because corresponding type is not more exist in model
                if(javaInput == null){
                    setInput(javaEditor);
                    refreshView();
                }
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
        if (!isLinkedWithEditor() || !isActive() || !isVisible
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
            if( ! updateSelection((ITextSelection) selection)){
                return;
            }
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
            if(service != null){
                service.removePostSelectionListener(editorListener);
            }
            FileBuffers.getTextFileBufferManager().removeFileBufferListener(
                editorListener);

        }
        if (textViewer != null && textViewer.getTextWidget() != null
            && !textViewer.getTextWidget().isDisposed()) {
            IDocument document= new Document("");
            textViewer.setDocument(document);
        }
        if (tableControl != null && !tableControl.isDisposed()) {
            setVerifyTableItems(null);
        }
        /*
        if(stackControl != null && !stackControl.isDisposed()){
            stackControl.setText("");
        }
        if(lvtControl != null && !lvtControl.isDisposed()){
            lvtControl.setText("");
        }
        */
        if(stackTable != null && !stackTable.isDisposed()){
            stackTable.removeAll();
        }
        if(lvtTable != null && !lvtTable.isDisposed()){
            lvtTable.removeAll();
        }
        if(statusControl != null && !statusControl.isDisposed() ){
            updateStatus(null, -1);
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
        IJavaElement childEl = getCurrentJavaElement();
        if(childEl == null && javaInput == null){
            setInput(javaEditor);
            childEl = javaInput;
        }
        boolean clearOutput = false;

        /*
         * TODO the followed code is crappy and should be overwritten
         * I can't understand some logic because this is not more clear at all.
         */
        if (scopeChanged || isJavaStructureChanged(childEl)) {
            bytecodeChanged = false;

            DecompiledClass result = decompileBytecode(childEl);
            if (result != null) {
                boolean verifyPossible = checkVerifyAction(childEl == null
                    ? javaInput
                    : childEl);
                if (!verifyCode || !verifyPossible) {
                    IDocument document= new Document(result.getText());
                    textViewer.setDocument(document);
                    // we are in verify mode but we can't show content because
                    // current element is abstract, so we clean table content
                    if(verifyCode){
                        setVerifyTableItems(null);
                    }
                    hasAnalyzerError = false;
                } else {
                    setVerifyTableItems(result.getTextTable());
                    List errors = result.getErrorLines();
                    if(errors.size() > 0){
                        // TODO this only changes color of status line -
                        // but it is possible also to provide useful info here...
                        hasAnalyzerError = true;
                        //currentErrorMessage = ...
                    }
                    for (int i = 0; i < errors.size(); ++i) {
                        int l = ((Integer) errors.get(i)).intValue();
                        tableControl.getItem(l).setForeground(errorColor);
                    }
                }
            } else {
                clearOutput = true;
            }
            lastDecompiledResult = result;
        } else {
            if(childEl == null && selectedOnly) {
                clearOutput = true;
            }
        }
        lastChildElement = childEl;
        if(clearOutput){
            if (!verifyCode) {
                // textControl.setText("");
                IDocument document= new Document("");
                textViewer.setDocument(document);
            } else {
                setVerifyTableItems(null);
            }
        }
        setSelectionInBytecodeView();
    }

    /**
     * @param result
     */
    private void updateStatus(DecompiledClass result, int bytecodeOffset) {
        // clear error messages, if any
        statusLineManager.setErrorMessage(null);
        if(result != null){
            currentStatusMessage = "Java:" + result.getAttribute("java.version")
            + " | class size:" + result.getAttribute("class.size");
        } else {
            currentStatusMessage = "";
        }
        String selectionInfo = "";
        if(bytecodeOffset >= 0){
            selectionInfo = " | offset:" + bytecodeOffset;
        }
        if(hasAnalyzerError){
            statusLineManager.setErrorMessage(currentStatusMessage + selectionInfo);
        } else {
            statusLineManager.setMessage( currentStatusMessage + selectionInfo);
        }
    }

    /**
     * @return IJavaElement which fits in the current selection in java editor
     */
    private IJavaElement getCurrentJavaElement() {
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
            // the exception is mostly occured if java structure was
            // changed and current element is not more exist in model
            // e.g. on rename/delete/move operation.
            // so it is not an error for user, but info for us
            BytecodeOutlinePlugin.log(e, IStatus.INFO);
            javaInput = null;
            lastChildElement = null;
            bytecodeChanged = true;
        }
        return childEl;
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
                    updateVerifierControl( decompiledLine);
                } else {
                    int lineCount = textControl.getLineCount();
                    if(decompiledLine < lineCount){
                        int offsetAtLine = textControl
                            .getOffsetAtLine(decompiledLine);
                        int offsetEnd = textControl.getText().indexOf(
                            '\n', offsetAtLine);
                        textControl.setSelection(offsetAtLine, offsetEnd);
                    }
                }
            } catch (IllegalArgumentException e) {
                BytecodeOutlinePlugin.error(null, e);
            }
        } else if (verifyCode) {
            // lvtControl.setText("");
            // stackControl.setText("");
            lvtTable.removeAll();
            stackTable.removeAll();
        }
        int bytecodeOffest = lastDecompiledResult.getBytecodeOffest(decompiledLine);
        updateStatus(lastDecompiledResult, bytecodeOffest);
    }

    private void updateVerifierControl(int decompiledLine) {
        lvtTable.removeAll();
        stackTable.removeAll();
        String[][][] frame = lastDecompiledResult.getFrameTables(
            decompiledLine, showQualifiedNames);
        if (frame != null) {
            for (int i = 0; i < frame[0].length; ++i) {
                if (frame[0][i] != null) {
                    new TableItem(lvtTable, SWT.NONE).setText(frame[0][i]);
                }
            }
            for (int i = 0; i < frame[1].length; ++i) {
                if (frame[1][i] != null) {
                    new TableItem(stackTable, SWT.NONE).setText(frame[1][i]);
                }
            }

            try {
                lvtTable.getColumn(0).pack();
                lvtTable.getColumn(1).pack();
                lvtTable.getColumn(2).pack();
            } catch (Exception e) {
                // TODO fix for Eclipse bug 84609, should be fixed in M5 "final"
            }
            try {
                stackTable.getColumn(0).pack();
                stackTable.getColumn(1).pack();
            } catch (Exception e) {
                // TODO fix for Eclipse bug 84609, should be fixed in M5 "final"
            }

        } else {
            // lvtControl.setText("");
            // stackControl.setText("");
        }
        tableControl.setSelection(decompiledLine);
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
                IRegion lineInfo = javaEditor.getViewer().getDocument()
                    .getLineInformation(sourceLine - 1);

                EclipseUtils.selectInEditor(javaEditor, lineInfo
                    .getOffset(), lineInfo.getLength());
            }
            if (verifyCode) {
              updateVerifierControl( decompiledLine);
            }
        } catch (Exception e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }

        int bytecodeOffest = lastDecompiledResult.getBytecodeOffest(decompiledLine);
        updateStatus(lastDecompiledResult, bytecodeOffest);
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

        /*
         * TODO the whole code is crappy and should be overwritten
         * I can't understand some logic because this is not more clear at all.
         */

        if (bytecodeChanged || lastDecompiledElement == null) {
            return true;
        }
        if (lastChildElement == null && childEl == null) {
            // no selected child - we stay by entire class bytecode
            return false;
        } else if(childEl == null && selectedOnly){
            return false;
        }
        if(selectedOnly){
            if (lastChildElement == null ||
                (lastChildElement != null && !lastChildElement.equals(childEl))){
                return true;
            }
        } else {
            if(lastChildElement == null && childEl != null){
                return true;
            }
            if( childEl != null && lastChildElement != null
                && !childEl.equals(lastChildElement)){
                IJavaElement ancestor = childEl.getAncestor(IJavaElement.TYPE);
                if(ancestor != null
                    && !ancestor.equals(lastChildElement.getAncestor(IJavaElement.TYPE))){
                    return true;
                }
            }
        }
        return false;
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
        if (type == null) {
            return null;
        }
        InputStream is = JdtUtils.createInputStream(type);
        lastDecompiledElement = type;
        if (is == null) {
            return null;
        }

        ClassLoader cl = null;
        if (verifyCode) {
            cl = JdtUtils.getClassLoader(type);
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
                    // this is compilation problem - don't show the message
                    BytecodeOutlinePlugin.log(e, IStatus.WARNING);
                }
            } else if (childEl.getElementType() == IJavaElement.METHOD) {
                IMethod iMethod = (IMethod) childEl;
                try {
                    methodName = JdtUtils.createMethodSignature(iMethod);
                } catch (JavaModelException e) {
                    // this is compilation problem - don't show the message
                    BytecodeOutlinePlugin.log(e, IStatus.WARNING);
                }
            }
        }

        DecompiledClass decompiledClass = null;
        int available = 0;
        try {
            available = is.available();
            decompiledClass = DecompilerClassVisitor.getDecompiledClass(
                is, fieldName, methodName, showQualifiedNames, isASMifierMode,
                verifyCode, cl);
        } catch (IOException e) {
            try {
                // check if compilation unit is ok - then this is the user problem
                if (type != null && type.isStructureKnown()) {
                    BytecodeOutlinePlugin.error(null, e);
                }
            } catch (JavaModelException e1) {
                // this is compilation problem - don't show the message
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
                BytecodeOutlinePlugin.log(e1, IStatus.WARNING);
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                BytecodeOutlinePlugin.log(e, IStatus.WARNING);
            }
        }
        if(decompiledClass != null){
            decompiledClass.setAttribute("class.size", "" + available);
        }
        return decompiledClass;
    }

    private void setVerifyTableItems(String[][] items) {
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
            try{
                tableControl.getColumn(0).pack();
                tableControl.getColumn(1).pack();
                tableControl.getColumn(2).pack();
            } catch (Exception e) {
                // TODO fix for Eclipse bug 84609, should be fixed in M5 "final"
            }
        }
    }

    public Object getAdapter(Class adapter) {
        if (IFindReplaceTarget.class.equals(adapter)) {
            return textViewer.getFindReplaceTarget();
        }
        if (Widget.class.equals(adapter)) {
            return textViewer.getTextWidget();
        }
        if (TextViewer.class.equals(adapter)) {
            return textViewer;
        }
        return super.getAdapter(adapter);
    }
    /**
     * Configures an action for key bindings.
     *
     * @param actionBars action bars for this page
     * @param actionID action definition id
     * @param action associated action
     */
    protected void setGlobalAction(IActionBars actionBars, String actionID,
        IAction action) {
        globalActions.put(actionID, action);
        actionBars.setGlobalActionHandler(actionID, action);
    }

    /**
     * Updates selection dependent actions.
     */
    protected void updateSelectionDependentActions() {
        Iterator iterator= selectionActions.iterator();
        while (iterator.hasNext()) {
            updateAction((String)iterator.next());
        }
    }

    /**
     * Updates the global action with the given id
     *
     * @param actionId action definition id
     */
    protected void updateAction(String actionId) {
        IAction action= (IAction)globalActions.get(actionId);
        if (action instanceof IUpdate) {
            ((IUpdate) action).update();
        }
    }

    protected void createActions() {
        IActionBars actionBars = getViewSite().getActionBars();
        TextViewerAction action = new TextViewerAction(
            textViewer, ITextOperationTarget.SELECT_ALL);

        action.configureAction(
            BytecodeOutlinePlugin.getResourceString(NLS_PREFIX
                + "select_all.label"), BytecodeOutlinePlugin
                .getResourceString(NLS_PREFIX + "select_all.tooltip"),
            BytecodeOutlinePlugin.getResourceString(NLS_PREFIX
                + "select_all.description"));
        setGlobalAction(actionBars, ActionFactory.SELECT_ALL.getId(), action);

        action = new TextViewerAction(textViewer, ITextOperationTarget.COPY);
        action.configureAction(
            BytecodeOutlinePlugin.getResourceString(NLS_PREFIX + "copy.label"),
            BytecodeOutlinePlugin
                .getResourceString(NLS_PREFIX + "copy.tooltip"),
            BytecodeOutlinePlugin.getResourceString(NLS_PREFIX
                + "copy.description"));
        action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
            .getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        action.setActionDefinitionId(IWorkbenchActionDefinitionIds.COPY);
        setGlobalAction(actionBars, ActionFactory.COPY.getId(), action);

        ResourceBundle bundle = BytecodeOutlinePlugin.getDefault().getResourceBundle();

        setGlobalAction(
            actionBars, ActionFactory.FIND.getId(), new FindReplaceAction(
                bundle, NLS_PREFIX + "find_replace.", this)); //$NON-NLS-1$

        selectionActions.add(ActionFactory.COPY.getId());
        selectionActions.add(ActionFactory.FIND.getId());

        actionBars.updateActionBars();
    }


    // orientation

  private void setOrientation(int orientation) {
    if( verifyControl == null || verifyControl.isDisposed()) {
      return;
    }

    boolean horizontal = orientation == VIEW_ORIENTATION_HORIZONTAL;
    verifyControl.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);

    for (int i = 0; i < toggleOrientationActions.length; ++i) {
      toggleOrientationActions[i].setChecked(orientation == toggleOrientationActions[i].getOrientation());
    }

    currentOrientation = orientation;
    // GridLayout layout= (GridLayout) fCounterComposite.getLayout();
    // setCounterColumns(layout);
    parent.layout();
  }


  void computeOrientation() {
    if (orientation != VIEW_ORIENTATION_AUTOMATIC) {
      currentOrientation= orientation;
      setOrientation(currentOrientation);
    } else {
      Point size= parent.getSize();
      if (size.x != 0 && size.y != 0) {
        setOrientation( size.x > size.y ? VIEW_ORIENTATION_HORIZONTAL : VIEW_ORIENTATION_VERTICAL);
      }
    }
  }

  private class ToggleOrientationAction extends Action {
    private final int actionOrientation;

    public ToggleOrientationAction(BytecodeOutlineView v, int orientation) {
      super("", AS_RADIO_BUTTON); //$NON-NLS-1$

      String symbolicName = BytecodeOutlinePlugin.getDefault().getBundle().getSymbolicName();
      if (orientation == VIEW_ORIENTATION_HORIZONTAL) {
        setText( BytecodeOutlinePlugin.getResourceString(NLS_PREFIX + "toggle.horizontal.label")); //$NON-NLS-1$
        setImageDescriptor( AbstractUIPlugin.imageDescriptorFromPlugin(symbolicName, "icons/th_horizontal.gif")); //$NON-NLS-1$
      } else if (orientation == VIEW_ORIENTATION_VERTICAL) {
        setText( BytecodeOutlinePlugin.getResourceString(NLS_PREFIX + "toggle.vertical.label")); //$NON-NLS-1$
        setImageDescriptor( AbstractUIPlugin.imageDescriptorFromPlugin(symbolicName, "icons/th_vertical.gif")); //$NON-NLS-1$
      } else if (orientation == VIEW_ORIENTATION_AUTOMATIC) {
        setText( BytecodeOutlinePlugin.getResourceString(NLS_PREFIX + "toggle.automatic.label"));  //$NON-NLS-1$
        setImageDescriptor( AbstractUIPlugin.imageDescriptorFromPlugin(symbolicName, "icons/th_automatic.gif")); //$NON-NLS-1$
      }
      actionOrientation= orientation;
      // WorkbenchHelp.setHelp(this, IJUnitHelpContextIds.RESULTS_VIEW_TOGGLE_ORIENTATION_ACTION);
    }

    public int getOrientation() {
      return actionOrientation;
    }

    public void run() {
      if (isChecked()) {
        orientation= actionOrientation;
        computeOrientation();
      }
    }
  }

}