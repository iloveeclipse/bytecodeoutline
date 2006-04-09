/*****************************************************************************************
 * Copyright (c) 2004 Andrei Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrei Loskutov -
 * initial API and implementation
 ****************************************************************************************/

package de.loskutov.bco.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IReusableEditor;

import de.loskutov.bco.preferences.BCOConstants;
import de.loskutov.bco.ui.actions.DefaultToggleAction;

/**
 */
public class BytecodeCompare extends CompareEditorInput {

    /** Stores reference to the element displayed on the left side of the viewer. */
    protected TypedElement left;
    /** Stores reference to the element displayed on the right side of the viewer. */
    protected TypedElement right;
    /** Action used in compare view/bytecode view to toggle asmifier mode on/off */
    protected Action toggleAsmifierModeAction;
    /** Action used in compare view/bytecode view to hide/show line info.  */
    protected Action hideLineInfoAction;
    /** Action used in compare view/bytecode view to hide/show local variables. */
    protected Action hideLocalsAction;

    protected IReusableEditor myEditor;

    /**
     * Constructor for PerforceCompareEditorInput.
     * @param left element displayed on the left.
     * @param right element displayed on the right.
     */
    public BytecodeCompare(TypedElement left, TypedElement right) {
        super(new CompareConfiguration());
        this.left = left;
        this.right = right;

        toggleAsmifierModeAction = new DefaultToggleAction(BCOConstants.SHOW_ASMIFIER_CODE,
            new IPropertyChangeListener(){
                public void propertyChange(PropertyChangeEvent event) {
                    if(IAction.CHECKED.equals(event.getProperty())){
                        boolean asmifier = Boolean.TRUE == event.getNewValue();
                        toggleMode(BCOConstants.F_SHOW_ASMIFIER_CODE, asmifier, asmifier);
                    }
                }
            });

        hideLineInfoAction = new DefaultToggleAction(BCOConstants.SHOW_LINE_INFO, new IPropertyChangeListener(){
                public void propertyChange(PropertyChangeEvent event) {
                    if(IAction.CHECKED.equals(event.getProperty())){
                        toggleMode(
                            BCOConstants.F_SHOW_LINE_INFO,
                            Boolean.TRUE == event.getNewValue(),
                            toggleAsmifierModeAction.isChecked());
                    }
                }
            });

        hideLocalsAction = new DefaultToggleAction(BCOConstants.SHOW_VARIABLES,
            new IPropertyChangeListener(){
                public void propertyChange(PropertyChangeEvent event) {
                    if(IAction.CHECKED.equals(event.getProperty())){
                        toggleMode(
                            BCOConstants.F_SHOW_VARIABLES,
                            Boolean.TRUE == event.getNewValue(),
                            toggleAsmifierModeAction.isChecked());
                    }
                }
            });
    }

    /** @see CompareEditorInput#prepareInput(IProgressMonitor) */
    protected Object prepareInput(IProgressMonitor monitor)
        throws InterruptedException {
        if (right == null || left == null) {
            return null;
        }

        try {
            initLabels();
            Differencer differencer = new Differencer();
            monitor.beginTask("Bytecode Outline: comparing...", 30); //$NON-NLS-1$
            IProgressMonitor sub = new SubProgressMonitor(monitor, 10);
            try {
                sub.beginTask("Bytecode Outline: comparing...", 100); //$NON-NLS-1$

                return differencer.findDifferences(
                    false, sub, null, null, left, right);
            } finally {
                sub.done();
            }
        } catch (OperationCanceledException e) {
            throw new InterruptedException(e.getMessage());
        } finally {
            monitor.done();
        }
    }

    /**
     * Sets up the title and pane labels for the comparison view.
     */
    private void initLabels() {
        CompareConfiguration cc = getCompareConfiguration();

        cc.setLeftLabel(left.getName());
        cc.setLeftImage(left.getImage());

        cc.setRightLabel(right.getName());
        cc.setRightImage(right.getImage());

        setTitle("Bytecode compare: "  //$NON-NLS-1$
            + left.getElementName() + " - " + right.getElementName()); //$NON-NLS-1$
    }


    /**
     * @see org.eclipse.compare.CompareEditorInput#createContents(org.eclipse.swt.widgets.Composite)
     */
    public Control createContents(Composite parent) {
        Object obj = parent.getData();

        // dirty hook on this place to get reference to editor
        // CompareEditor extends EditorPart implements IReusableEditor
        if(obj instanceof IReusableEditor){
            myEditor = (IReusableEditor)obj;
        }

        Control control = super.createContents(parent);

        // dirty hook on this place to get reference to CompareViewerPane
        // from CompareEditorInput: see field
        // private CompareViewerSwitchingPane fContentInputPane;
        // see also CompareEditorInput.createContents:
        //  fComposite.setData("Nav", //$NON-NLS-1$
        Object obj2 = control.getData("Nav");  //$NON-NLS-1$
        if (obj2 instanceof CompareViewerSwitchingPane[]) {
            // there are 4 panels, last one is the input pane that we search for
            CompareViewerSwitchingPane[] panels = (CompareViewerSwitchingPane[])obj2;
            if(panels.length > 0){
                Composite comparePane = panels[panels.length-1];
                ToolBarManager toolBarManager2 = CompareViewerPane
                    .getToolBarManager(comparePane);
                boolean separatorExist = false;
                if (toolBarManager2.find(hideLineInfoAction.getId()) == null) {
                    if(!separatorExist) {
                        separatorExist = true;
                        toolBarManager2.insert(0, new Separator("bco")); //$NON-NLS-1$
                    }
                    toolBarManager2.insertBefore("bco", hideLineInfoAction); //$NON-NLS-1$
                    toolBarManager2.update(true);
                }
                if (toolBarManager2.find(hideLocalsAction.getId()) == null) {
                    if(!separatorExist) {
                        separatorExist = true;
                        toolBarManager2.insert(0, new Separator("bco")); //$NON-NLS-1$
                    }
                    toolBarManager2.insertBefore("bco", hideLocalsAction); //$NON-NLS-1$
                    toolBarManager2.update(true);
                }
                if (toolBarManager2.find(toggleAsmifierModeAction.getId()) == null) {
                    if(!separatorExist) {
                        toolBarManager2.insert(0, new Separator("bco")); //$NON-NLS-1$
                        separatorExist = true;
                    }
                    toolBarManager2.insertBefore("bco", toggleAsmifierModeAction); //$NON-NLS-1$
                    toolBarManager2.update(true);
                }
            }
        }

        return control;
    }

    protected void toggleMode(int mode, boolean value, boolean isASMifierMode) {
        String contentType = isASMifierMode
            ? TypedElement.TYPE_ASM_IFIER
            : TypedElement.TYPE_BYTECODE;

        left.setMode(mode, value);
        left.setMode(BCOConstants.F_SHOW_ASMIFIER_CODE, isASMifierMode);
        left.setType(contentType);

        right.setMode(mode, value);
        right.setMode(BCOConstants.F_SHOW_ASMIFIER_CODE, isASMifierMode);
        right.setType(contentType);
        CompareUI.reuseCompareEditor(this, myEditor);
    }
}