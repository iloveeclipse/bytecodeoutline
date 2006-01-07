/*****************************************************************************************
 * Copyright (c) 2004 Andrei Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrei Loskutov -
 * initial API and implementation
 ****************************************************************************************/

package de.loskutov.bco.compare;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.graphics.Image;

import de.loskutov.bco.BytecodeOutlinePlugin;
import de.loskutov.bco.asm.DecompiledClass;
import de.loskutov.bco.asm.DecompilerClassVisitor;
import de.loskutov.bco.ui.JdtUtils;

/**
 * @author Andrei
 */
public class TypedElement extends BufferedContent
    implements
        ITypedElement,
        IStructureComparator {

    private String name;

    private String type;

    private IJavaElement element;

    private boolean isASMifierMode;

    /** used by Eclipse to recognize appropriated viewer */
    public static final String TYPE_BYTECODE = "bytecode";

    /** used by Eclipse to recognize appropriated viewer */
    public static final String TYPE_ASM_IFIER = "java";

    /**
     * Constructor for TypedElement.
     */
    public TypedElement() {
        super();
    }

    /**
     * Constructor for TypedElement.
     * @param name
     * @param type
     * @param element
     */
    public TypedElement(String name, String type, IJavaElement element) {
        this();
        this.name = name;
        this.type = type;
        this.element = element;
    }

    /**
     * @return Returns the isASMifierMode.
     */
    protected boolean isASMifierMode() {
        return isASMifierMode;
    }

    /**
     * @param isASMifierMode The isASMifierMode to set.
     */
    protected void setASMifierMode(boolean isASMifierMode) {
        this.isASMifierMode = isASMifierMode;
    }

    /**
     * @see org.eclipse.compare.ITypedElement#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * @see org.eclipse.compare.ITypedElement#getType()
     */
    public String getType() {
        return type;
    }

    /**
     * @param type The type to set.
     */
    protected void setType(String type) {
        this.type = type;
    }

    /**
     * @return name
     */
    public String getElementName() {
        return JdtUtils.getElementName(element);
    }

    public Image getImage() {
        // default image for .class files
        return CompareUI.getImage("class");
    }

    public Object[] getChildren() {
        return new TypedElement[0];
    }

    protected InputStream createStream() throws CoreException {
        InputStream stream = JdtUtils.createInputStream(element);
        if (stream == null) {
            throw new CoreException(new Status(
                IStatus.ERROR, "de.loskutov.bco", -1,
                "cannot get bytecode from class file", null));
        }
        DecompiledClass decompiledClass = null;
        try {
            decompiledClass = DecompilerClassVisitor.getDecompiledClass(
                stream, null, null, true, isASMifierMode(), false, null);
        } catch (IOException e) {
            throw new CoreException(new Status(
                IStatus.ERROR, "de.loskutov.bco", -1,
                "cannot get bytecode dump", null));
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                BytecodeOutlinePlugin.log(e, IStatus.WARNING);
            }
        }
        byte[] bytes = decompiledClass.getText().getBytes();
        // use internal buffering to prevent multiple calls to this method
        setContent(bytes);
        return new ByteArrayInputStream(bytes);
    }
}