
package de.loskutov.bco.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

import de.loskutov.bco.BytecodeOutlinePlugin;


/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */
public class BCOPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public BCOPreferencePage() {
    super( GRID );
    setPreferenceStore(BytecodeOutlinePlugin.getDefault()
            .getPreferenceStore());
    setDescription(BytecodeOutlinePlugin.getResourceString("BCOPreferencePage.defaultsGroup"));
  }

  /*
   * Creates the field editors. Field editors are abstractions of the common GUI
   * blocks needed to manipulate various types of preferences. Each field editor
   * knows how to save and restore itself.
   */
  public void createFieldEditors() {

    addField( new BooleanFieldEditor( BCOConstants.LINK_VIEW_TO_EDITOR, 
        BytecodeOutlinePlugin.getResourceString( "BCOPreferencePage.linkViewToEditor" ), //$NON-NLS-1$
        getFieldEditorParent() ) );

    addField( new BooleanFieldEditor( BCOConstants.SHOW_ONLY_SELECTED_ELEMENT, 
        BytecodeOutlinePlugin.getResourceString( "BCOPreferencePage.showOnlySelected" ), //$NON-NLS-1$
        getFieldEditorParent() ) );
    
    addField( new BooleanFieldEditor( BCOConstants.SHOW_RAW_BYTECODE, 
        BytecodeOutlinePlugin.getResourceString( "BCOPreferencePage.showRawBytecode" ), //$NON-NLS-1$
        getFieldEditorParent() ) );
    
    addField( new BooleanFieldEditor( BCOConstants.SHOW_ASMIFIER_CODE, 
        BytecodeOutlinePlugin.getResourceString( "BCOPreferencePage.showAsmifierCode" ), //$NON-NLS-1$
        getFieldEditorParent() ) );
    
    addField( new BooleanFieldEditor( BCOConstants.SHOW_ANALYZER, 
        BytecodeOutlinePlugin.getResourceString( "BCOPreferencePage.showAnalyzer" ), //$NON-NLS-1$
        getFieldEditorParent() ) );

    //
    addField( new BooleanFieldEditor( BCOConstants.SHOW_LINE_INFO, 
        BytecodeOutlinePlugin.getResourceString( "BCOPreferencePage.showLineInfo" ), //$NON-NLS-1$
        getFieldEditorParent() ) );
    
    addField( new BooleanFieldEditor( BCOConstants.SHOW_VARIABLES, 
        BytecodeOutlinePlugin.getResourceString( "BCOPreferencePage.showVariables" ), //$NON-NLS-1$
        getFieldEditorParent() ) );
    
    addField( new BooleanFieldEditor( BCOConstants.SHOW_STACKMAP, 
        BytecodeOutlinePlugin.getResourceString( "BCOPreferencePage.showStackMap" ), //$NON-NLS-1$
        getFieldEditorParent() ) );
    
    addField( new BooleanFieldEditor( BCOConstants.EXPAND_STACKMAP,
        BytecodeOutlinePlugin.getResourceString( "BCOPreferencePage.expandStackMap" ), //$NON-NLS-1$
        getFieldEditorParent() ) );
    
    // addField( new BooleanFieldEditor( BCOConstants.RECALCULATE_STACKMAP,
    //    BytecodeOutlinePlugin.getResourceString( "BCOPreferencePage.recalculateStackMap" ), //$NON-NLS-1$
    //    getFieldEditorParent() ) );
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init( IWorkbench workbench ) {
      //
  }

}
