/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.bco.preferences;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import de.loskutov.bco.BytecodeOutlinePlugin;


/**
 * @author Andrei
 */
public class SupportPanel {

    static void createSupportLinks(Composite defPanel) {
        Group commonPanel = new Group(defPanel, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth = 0;
        commonPanel.setLayout(layout);
        commonPanel.setLayoutData(gridData);
        String pluginName = "BytecodeOutline";
        commonPanel.setText("Support " + pluginName+ " plugin and get support too :-)");

        Label label = new Label(commonPanel, SWT.NONE);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        label.setText("Feel free to support " + pluginName+ " plugin in the way you like:");

        Font font = JFaceResources.getFontRegistry().getBold(
                JFaceResources.getDialogFont().getFontData()[0].getName());

        Link link = new Link(commonPanel, SWT.NONE);
        link.setFont(font);
        link.setText(" - <a>visit homepage</a>");
        link.setToolTipText("You need just a sense of humor!");
        link.addListener (SWT.Selection, new Listener () {
            @Override
            public void handleEvent(Event event) {
                handleUrlClick("http://andrei.gmxhome.de/bytecode");
            }
        });

        link = new Link(commonPanel, SWT.NONE);
        link.setFont(font);
        link.setText(" - <a>report issue or feature request</a>");
        link.setToolTipText("You need a valid account at ow2.org!");
        link.addListener (SWT.Selection, new Listener () {
            @Override
            public void handleEvent(Event event) {
                handleUrlClick("http://forge.ow2.org/tracker/?group_id=23");
            }
        });

        link = new Link(commonPanel, SWT.NONE);
        link.setFont(font);
        link.setText(" - <a>add to your favorites at Eclipse MarketPlace</a>");
        link.setToolTipText("You need a valid bugzilla account at Eclipse.org!");
        link.addListener (SWT.Selection, new Listener () {
            @Override
            public void handleEvent(Event event) {
                handleUrlClick("http://marketplace.eclipse.org/content/bytecode-outline");
            }
        });

        link = new Link(commonPanel, SWT.NONE);
        link.setFont(font);
        link.setText(" - <a>make a donation to support plugin development</a>");
        link.setToolTipText("You do NOT need a PayPal account!");
        link.addListener (SWT.Selection, new Listener () {
            @Override
            public void handleEvent(Event event) {
                handleUrlClick("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=R5SHJLNGUXKHU");
            }
        });
    }

    private static void handleUrlClick(final String urlStr) {
        try {
            IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser externalBrowser = support.getExternalBrowser();
            if(externalBrowser != null){
                externalBrowser.openURL(new URL(urlStr));
            } else {
                IWebBrowser browser = support.createBrowser(urlStr);
                if(browser != null){
                    browser.openURL(new URL(urlStr));
                }
            }
        } catch (PartInitException e) {
            BytecodeOutlinePlugin.error("Failed to open url " + urlStr, e);
        } catch (MalformedURLException e) {
            BytecodeOutlinePlugin.error("Failed to open url " + urlStr, e);
        }
    }
}
