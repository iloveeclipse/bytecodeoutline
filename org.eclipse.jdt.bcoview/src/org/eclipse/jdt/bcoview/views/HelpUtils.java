/*******************************************************************************
 * Copyright (c) 2018 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import org.objectweb.asm.util.Printer;

public class HelpUtils {
	// TODO: configure it via preference
	private static final String SPECS_HTML = "https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html"; //$NON-NLS-1$
	private static String fullSpec;
	private static String htmlHead;

	private static String checkOpcodeName(String opcodeName) {
		opcodeName = opcodeName.toLowerCase();
		/*
		 * we need an additional check for DCONST_1...5, FCONST_1...5 etc case
		 * to convert it to DCONST_D etc
		 */
		int sepIndex = opcodeName.indexOf('_');
		if(sepIndex > 0) {
			if (Character.isDigit(opcodeName.charAt(sepIndex + 1))) {
				opcodeName = opcodeName.substring(0, sepIndex);
				switch(opcodeName.charAt(0)){
					case 'd':
						opcodeName += "_<d&gt;"; //$NON-NLS-1$
						break;
					case 'f':
						opcodeName += "_<f&gt;"; //$NON-NLS-1$
						break;
					case 'l':
						opcodeName += "_<l&gt;"; //$NON-NLS-1$
						break;
					default:
						// ICONST uses "n"
						opcodeName += "_<n&gt;"; //$NON-NLS-1$
						break;
				}
			}
			if(opcodeName.startsWith("if_acmp")) { //$NON-NLS-1$
				opcodeName = "if_acmp<cond&gt;"; //$NON-NLS-1$
			} else
				if(opcodeName.startsWith("if_icmp")) { //$NON-NLS-1$
					opcodeName = "if_icmp<cond&gt;"; //$NON-NLS-1$
				} else
					if(opcodeName.startsWith("if_")) { //$NON-NLS-1$
						opcodeName = "if<cond&gt;"; //$NON-NLS-1$
					} else
						if(opcodeName.startsWith("aload_")) { //$NON-NLS-1$
							opcodeName = "aload_<n&gt;"; //$NON-NLS-1$
						} else
							if(opcodeName.startsWith("iconst_")) { //$NON-NLS-1$
								opcodeName = "iconst_<i&gt;"; //$NON-NLS-1$
							}
		}  else
			if(opcodeName.startsWith("if")) { //$NON-NLS-1$
				opcodeName = "if<cond&gt;"; //$NON-NLS-1$
			}
		return opcodeName;
	}

	private static String getOpcodeName(int opcode) {
		if(opcode < 0 || opcode >= Printer.OPCODES.length) {
			return null;
		}
		String opcodeName = Printer.OPCODES[opcode];
		if(opcodeName == null) {
			return null;
		}
		return opcodeName;
	}

	private static URL toUrl(String href) {
		try {
			return new URL(href);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public static URL getHelpIndex() {
		return toUrl(SPECS_HTML);
	}

	private static String readFullSpec() {
		URL helpResource = toUrl(SPECS_HTML);
		StringBuilder sb = new StringBuilder();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(helpResource.openStream(), "UTF-8"))){ //$NON-NLS-1$
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} catch (IOException e) {
			sb.append("Error trying access JVM specification at ").append(SPECS_HTML); //$NON-NLS-1$
			sb.append(":"); //$NON-NLS-1$
			sb.append(e);
		}
		return sb.toString();
	}

	public static StringBuilder getOpcodeHelpFor(int opcode) {
		if(fullSpec == null) {
			fullSpec = readFullSpec();
			htmlHead = readHtmlHead();
		}
		StringBuilder sb = new StringBuilder();
		String opcodeName = getOpcodeName(opcode);
		if(opcodeName == null) {
			return sb;
		}
		return getOpcodeHelpFor(opcodeName);
	}

	public static StringBuilder getOpcodeHelpFor(String opcodeName) {
		if(fullSpec == null) {
			fullSpec = readFullSpec();
			htmlHead = readHtmlHead();
		}
		StringBuilder sb = new StringBuilder();
		if (opcodeName != null) {
			opcodeName = checkOpcodeName(opcodeName);
		}
		sb.append(htmlHead);

		// Extract only important part related to the given opcode
		String patternStart = "<div class=\"section-execution\" title=\"" + opcodeName + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		String patternEnd = "<div class=\"section-execution\" title=\""; //$NON-NLS-1$
		try (Scanner in = new Scanner(fullSpec)){
			String line;
			boolean foundStart = false;
			boolean checkEnd = false;
			while (in.hasNextLine()) {
				line = in.nextLine();
				if(checkEnd && line.contains(patternEnd)) {
					break;
				}
				if(!foundStart && line.contains(patternStart)) {
					foundStart = true;
					checkEnd = true;
				}
				if(foundStart) {
					sb.append(line);
				}
			}
		}

		// Allow navigation relative to the document
		int endHeadIdx= sb.indexOf("</head>"); //$NON-NLS-1$
		if(endHeadIdx > 0) {
			sb.insert(endHeadIdx, "\n<base href='" + SPECS_HTML + "'>\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sb.append("</body></html>"); //$NON-NLS-1$
		return sb;
	}

	private static String readHtmlHead() {
		StringBuilder sb = new StringBuilder();
		if(fullSpec == null) {
			return sb.toString();
		}
		try (Scanner in = new Scanner(fullSpec)){
			String line;
			while (in.hasNextLine()) {
				line = in.nextLine();
				if(line.contains("<body")) { //$NON-NLS-1$
					sb.append(line.substring(0, line.indexOf("<body"))); //$NON-NLS-1$
					break;
				}
				sb.append(line);
			}
		}
		return sb.toString();
	}

}
