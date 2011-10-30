/*******************************************************************************
 * Copyright (c) 2011 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.bco.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.objectweb.asm.util.Printer;
import org.osgi.framework.Bundle;

import de.loskutov.bco.BytecodeOutlinePlugin;


public class HelpUtils {
    private static String styleSheet;
    private static final String DOC_BASE = "/" + BytecodeOutlinePlugin.getDefault().getBundle().getSymbolicName() + "/doc/";
    private static RGB bg_color_rgb = new RGB(255, 255, 255);
    private static RGB fg_color_rgb = new RGB(0, 0, 0);

    static {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                bg_color_rgb = Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND).getRGB();
                fg_color_rgb = Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND).getRGB();
            }
        });
    }

    private static String checkOpcodeName(String opcodeName) {
        opcodeName = opcodeName.toLowerCase();
        /*
         * we need an additional check for DCONST_1...5, FCONST_1...5 etc case
         * to convert it to DCONST_D etc
         */
        int sepIndex = opcodeName.indexOf('_');
        if(sepIndex > 0 && Character.isDigit(opcodeName.charAt(sepIndex + 1))){
            opcodeName = opcodeName.substring(0, sepIndex);
            switch(opcodeName.charAt(0)){
                case 'd':
                    opcodeName += "_d";
                    break;
                case 'f':
                    opcodeName += "_f";
                    break;
                case 'l':
                    opcodeName += "_l";
                    break;
                default:
                    // ICONST uses "n"
                    opcodeName += "_n";
                    break;
            }
        }
        return opcodeName;
    }

    public static URL getHelpResource(int opcode) {
        if(opcode < 0 || opcode >= Printer.OPCODES.length) {
            return null;
        }
        String opcodeName = Printer.OPCODES[opcode];
        if (opcodeName != null) {
            opcodeName = checkOpcodeName(opcodeName);
        }
        if(opcodeName == null) {
            return null;
        }
        return getHelpResource(opcodeName);
    }

    private static URL getHelpResource(String name) {
        String href = DOC_BASE + "ref-" + name + ".html";
        return resolveToHelpUrl(href);
    }

    public static URL getHelpIndex() {
        String href = DOC_BASE + "opcodes.html";
        return resolveToHelpUrl(href);
    }

    private static String getDocBase() {
        URL base = resolveToHelpUrl(DOC_BASE);
        if (base != null) {
            return base.toString();
        }
        return null;
    }

    private static URL resolveToHelpUrl(String path) {
        try {
            return BaseHelpSystem.resolve(path, true);
        } catch (Exception e) {
            return null;
        }
    }

    private static void appendColor(StringBuilder buffer, RGB rgb) {
        buffer.append('#');
        appendAsHexString(buffer, rgb.red);
        appendAsHexString(buffer, rgb.green);
        appendAsHexString(buffer, rgb.blue);
    }

    private static void appendAsHexString(StringBuilder buffer, int intValue) {
        String hexValue= Integer.toHexString(intValue);
        if (hexValue.length() == 1) {
            buffer.append('0');
        }
        buffer.append(hexValue);
    }


    /**
     * From {@link JavadocHover} class: returns the Javadoc hover style sheet with the
     * current Javadoc font from the preferences.
     * @return the updated style sheet
     */
    public static String getHelpStyleSheet() {
        if (styleSheet == null) {
            styleSheet = loadStyleSheet();
        }
        String css = styleSheet;
        if (css == null || css.isEmpty()) {
            return "";
        }
        FontData fontData= JFaceResources.getFontRegistry().getFontData(
            PreferenceConstants.APPEARANCE_JAVADOC_FONT)[0];
        css = HTMLPrinter.convertTopLevelFont(css, fontData);
        StringBuilder sb = new StringBuilder(css);
        sb.append("\nbody {  background-color:");
        appendColor(sb, bg_color_rgb);
        sb.append(";  color:");
        appendColor(sb, fg_color_rgb);
        sb.append(";  }\n");
        return sb.toString();
    }



    /**
     * From {@link JavadocHover} class: loads and returns the Javadoc hover style sheet.
     * @return the style sheet, or empty string if unable to load
     */
    private static String loadStyleSheet() {
        Bundle bundle= Platform.getBundle(JavaPlugin.getPluginId());
        URL styleSheetURL= bundle.getEntry("/JavadocHoverStyleSheet.css"); //$NON-NLS-1$
        if (styleSheetURL == null) {
            return "";
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(styleSheetURL.openStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder(1500);
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        } catch (IOException ex) {
            return "";
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                //
            }
        }
    }

    public static StringBuilder getOpcodeHelpFor(int opcode) {
        URL helpResource = getHelpResource(opcode);
        StringBuilder sb = new StringBuilder();
        if(helpResource == null) {
            return sb;
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(helpResource.openStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            return sb;
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    //
                }
            }
        }
        int styleEnd = sb.indexOf("</style>");
        if(styleEnd > 0) {
            sb.insert(styleEnd, getHelpStyleSheet());
        }
        int endHeadIdx= sb.indexOf("</head>"); //$NON-NLS-1$
        String base = getDocBase();
        if(base != null) {
            sb.insert(endHeadIdx, "\n<base href='" + base + "'>\n");
        }
        return sb;
    }

}
