/*******************************************************************************
 * Copyright (c) 2006 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.bco.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.loskutov.bco.BytecodeOutlinePlugin;

/**
 *
 */
public class BCOPreferencePage extends PreferencePage
    implements
        IWorkbenchPreferencePage,
        SelectionListener {

    private static final String TOOLTIP_SUFFIX = "Tip";

    private Button expandStackMapCheck;
    private Button showAnalyzerCheck;
    private Button recalculateStackMapCheck;
    private Button showVariablesCheck;
    private Button showLineInfoCheck;
    private Button showRawBytecodeCheck;
    private Button showAsmifierCodeCheck;
    private Button showOnlySelectedCheck;
    private Button linkViewToEditorCheck;

    public BCOPreferencePage() {
        super();
        setPreferenceStore(BytecodeOutlinePlugin.getDefault()
            .getPreferenceStore());
    }

    /*
     * @see PreferencePage#createControl(Composite)
     */
    public void createControl(Composite parent) {
        super.createControl(parent);
    }

    /*
     * @see PreferencePage#createContents(Composite)
     */
    protected Control createContents(Composite parent) {
        Composite composite = createContainer(parent);
        GridLayout gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        composite.setLayout(gridLayout);

        Composite defPanel = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        defPanel.setLayout(layout);
        GridData gridData = new GridData(GridData.FILL_BOTH
            | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        defPanel.setLayoutData(gridData);

        // -------------------------------------------------------------------------------
        Group defaultsComposite = new Group(defPanel, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        defaultsComposite.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL
            | GridData.GRAB_HORIZONTAL);
        defaultsComposite.setLayoutData(gridData);

        defaultsComposite.setText(BytecodeOutlinePlugin
            .getResourceString("BCOPreferencePage.defaultsGroup"));

        expandStackMapCheck = createLabeledCheck(
            "BCOPreferencePage.expandStackMap", getPreferenceStore()
                .getBoolean(BCOConstants.EXPAND_STACKMAP), defaultsComposite);

        recalculateStackMapCheck = createLabeledCheck(
            "BCOPreferencePage.recalculateStackMap", getPreferenceStore()
                .getBoolean(BCOConstants.RECALCULATE_STACKMAP),
            defaultsComposite);

        showVariablesCheck = createLabeledCheck(
            "BCOPreferencePage.showVariables", getPreferenceStore().getBoolean(
                BCOConstants.SHOW_VARIABLES), defaultsComposite);

        showLineInfoCheck = createLabeledCheck(
            "BCOPreferencePage.showLineInfo", getPreferenceStore().getBoolean(
                BCOConstants.SHOW_LINE_INFO), defaultsComposite);

        showRawBytecodeCheck = createLabeledCheck(
            "BCOPreferencePage.showRawBytecode", getPreferenceStore()
                .getBoolean(BCOConstants.SHOW_RAW_BYTECODE), defaultsComposite);

        showAnalyzerCheck = createLabeledCheck(
            "BCOPreferencePage.showAnalyzer", getPreferenceStore()
            .getBoolean(BCOConstants.SHOW_ANALYZER), defaultsComposite);

        showAsmifierCodeCheck = createLabeledCheck(
            "BCOPreferencePage.showAsmifierCode", getPreferenceStore()
            .getBoolean(BCOConstants.SHOW_ASMIFIER_CODE), defaultsComposite);

        showOnlySelectedCheck = createLabeledCheck(
            "BCOPreferencePage.showOnlySelected", getPreferenceStore()
            .getBoolean(BCOConstants.SHOW_ONLY_SELECTED_ELEMENT), defaultsComposite);

        linkViewToEditorCheck = createLabeledCheck(
            "BCOPreferencePage.linkViewToEditor", getPreferenceStore()
            .getBoolean(BCOConstants.LINK_VIEW_TO_EDITOR), defaultsComposite);
        return composite;
    }

    private Composite createContainer(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
            | GridData.HORIZONTAL_ALIGN_FILL));
        return composite;
    }

    /*
     * @see IWorkbenchPreferencePage#init(IWorkbench)
     */
    public void init(IWorkbench workbench) {
        // ignored
    }

    /*
     * @see SelectionListener#widgetDefaultSelected(SelectionEvent)
     */
    public void widgetDefaultSelected(SelectionEvent selectionEvent) {
        widgetSelected(selectionEvent);
    }

    /*
     * @see SelectionListener#widgetSelected(SelectionEvent)
     */
    public void widgetSelected(SelectionEvent selectionEvent) {
        // ignored
    }

    public boolean performOk() {
        IPreferenceStore store = getPreferenceStore();

        store.setValue(BCOConstants.SHOW_ANALYZER, showAnalyzerCheck
            .getSelection());
        store.setValue(BCOConstants.EXPAND_STACKMAP, expandStackMapCheck
            .getSelection());
        store.setValue(
            BCOConstants.RECALCULATE_STACKMAP, recalculateStackMapCheck
                .getSelection());
        store.setValue(BCOConstants.SHOW_VARIABLES, showVariablesCheck
            .getSelection());
        store.setValue(BCOConstants.SHOW_LINE_INFO, showLineInfoCheck
            .getSelection());
        store.setValue(BCOConstants.SHOW_RAW_BYTECODE, showRawBytecodeCheck
            .getSelection());
        store.setValue(BCOConstants.SHOW_ASMIFIER_CODE, showAsmifierCodeCheck
            .getSelection());
        store.setValue(
            BCOConstants.SHOW_ONLY_SELECTED_ELEMENT, showOnlySelectedCheck
                .getSelection());
        store.setValue(
            BCOConstants.LINK_VIEW_TO_EDITOR, linkViewToEditorCheck
                .getSelection());

        return true;
    }

    private Button createLabeledCheck(String titleId, boolean value,
        Composite defPanel) {
        Button fButton = new Button(defPanel, SWT.CHECK | SWT.LEFT);
        GridData data = new GridData();
        fButton.setLayoutData(data);
        fButton.setText(BytecodeOutlinePlugin.getResourceString(titleId));
        fButton.setSelection(value);
        fButton.setToolTipText(BytecodeOutlinePlugin.getResourceString(titleId
            + TOOLTIP_SUFFIX));
        return fButton;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults() {
        super.performDefaults();
        IPreferenceStore store = getPreferenceStore();

        showAnalyzerCheck.setSelection(store
            .getDefaultBoolean(BCOConstants.SHOW_ANALYZER));

        expandStackMapCheck.setSelection(store
            .getDefaultBoolean(BCOConstants.EXPAND_STACKMAP));
        recalculateStackMapCheck.setSelection(store
            .getDefaultBoolean(BCOConstants.RECALCULATE_STACKMAP));
        showVariablesCheck.setSelection(store
            .getDefaultBoolean(BCOConstants.SHOW_VARIABLES));
        showLineInfoCheck.setSelection(store
            .getDefaultBoolean(BCOConstants.SHOW_LINE_INFO));
        showRawBytecodeCheck.setSelection(store
            .getDefaultBoolean(BCOConstants.SHOW_RAW_BYTECODE));

        showAsmifierCodeCheck.setSelection(store
            .getDefaultBoolean(BCOConstants.SHOW_ASMIFIER_CODE));
        showOnlySelectedCheck.setSelection(store
            .getDefaultBoolean(BCOConstants.SHOW_ONLY_SELECTED_ELEMENT));
        linkViewToEditorCheck.setSelection(store
            .getDefaultBoolean(BCOConstants.LINK_VIEW_TO_EDITOR));
    }

    public void dispose() {
        if (showAnalyzerCheck != null) {
            showAnalyzerCheck.dispose();
            expandStackMapCheck.dispose();
            recalculateStackMapCheck.dispose();
            showVariablesCheck.dispose();
            showLineInfoCheck.dispose();
            showRawBytecodeCheck.dispose();
            showAsmifierCodeCheck.dispose();
            showOnlySelectedCheck.dispose();
            linkViewToEditorCheck.dispose();
        }
        super.dispose();
    }
}