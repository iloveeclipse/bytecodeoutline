/*****************************************************************************************
 * Copyright (c) 2004 Andrei Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrei Loskutov -
 * initial API and implementation
 ****************************************************************************************/
package de.loskutov.bco;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class BytecodeOutlinePlugin extends AbstractUIPlugin {
    //The shared instance.
    private static BytecodeOutlinePlugin plugin;
    //Resource bundle.
    private ResourceBundle resourceBundle;
    public static boolean DEBUG;

    /**
     * The constructor.
     */
    public BytecodeOutlinePlugin() {
        super();
        if(plugin != null){
            throw new IllegalStateException("Bytecode outline plugin is a singleton!");
        }
        plugin = this;
        try {
            resourceBundle = ResourceBundle
                .getBundle("de.loskutov.bco.BytecodeOutlinePluginResources"); //$NON-NLS-1$
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
    }

    /**
     * This method is called upon plug-in activation
     * @param context
     * @throws Exception
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        DEBUG = isDebugging();
    }

    /**
     * This method is called when the plug-in is stopped
     * @param context
     * @throws Exception
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     * @return plugin
     */
    public static BytecodeOutlinePlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not found.
     * @param key
     * @return translation
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = BytecodeOutlinePlugin.getDefault()
            .getResourceBundle();
        try {
            return (bundle != null)
                ? bundle.getString(key)
                : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * Returns the workspace instance.
     * @return shell object
     */
    public static Shell getShell() {
        return getDefault().getWorkbench().getActiveWorkbenchWindow()
            .getShell();
    }

    /**
     * @param messageID
     * @param error
     */
    public static void error(String messageID, Throwable error) {
        Shell shell = getShell();
        String message = getResourceString("BytecodeOutline.Error"); //$NON-NLS-1$
        if (messageID != null) {
            message = getResourceString(messageID);
        }
        if (error != null) {
            message += " " + error.getMessage();
        }
        MessageDialog.openError(
            shell, getResourceString("BytecodeOutline.Title"), //$NON-NLS-1$
            message);

        getDefault().getLog().log(
            new Status(IStatus.ERROR, "BytecodeOutline", 0, message, error)); //$NON-NLS-1$
    }

    /**
     * @param statusID one of IStatus. constants like IStatus.ERROR etc
     * @param error
     */
    public static void log(Throwable error, int statusID) {
        String message = error.getMessage();
        if(message == null){
            message = error.toString();
        }
        getDefault().getLog()
            .log(
                new Status(
                    statusID,
                    "BytecodeOutline", 0, message, error)); //$NON-NLS-1$
    }


}
