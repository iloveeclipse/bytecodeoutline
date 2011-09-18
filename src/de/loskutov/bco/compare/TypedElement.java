/*****************************************************************************************
 * Copyright (c) 2011 Andrey Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrey Loskutov -
 * initial API and implementation
 ****************************************************************************************/

package de.loskutov.bco.compare;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.BitSet;

import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.loskutov.bco.BytecodeOutlinePlugin;
import de.loskutov.bco.asm.DecompiledClass;
import de.loskutov.bco.asm.DecompilerHelper;
import de.loskutov.bco.asm.DecompilerOptions;
import de.loskutov.bco.ui.JdtUtils;

/**
 * @author Andrei
 */
public class TypedElement extends BufferedContent
    implements
        ITypedElement,
        IStructureComparator {

    private final String name;

    private String type;

    private final String methodName;

    private final IJavaElement element;

    /** used by Eclipse to recognize appropriated viewer */
    public static final String TYPE_BYTECODE = "bytecode";

    /** used by Eclipse to recognize appropriated viewer */
    public static final String TYPE_ASM_IFIER = "java";

    private final BitSet modes;

    /**
     * Constructor for TypedElement.
     * @param name
     * @param type
     * @param element
     * @param modes
     */
    public TypedElement(String name, String methodName, String type, IJavaElement element, BitSet modes) {
        super();
        this.name = name;
        this.methodName = methodName;
        this.type = type;
        this.element = element;
        this.modes = modes;
    }

    /**
     * @see org.eclipse.compare.ITypedElement#getName()
     */
    @Override
    public String getName() {
        return name;
    }


    /**
     * @see org.eclipse.compare.ITypedElement#getType()
     */
    @Override
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

    @Override
    public Image getImage() {
        // default image for .class files
        return CompareUI.getImage("class");
    }

    @Override
    public Object[] getChildren() {
        return new TypedElement[0];
    }

    @Override
    protected InputStream createStream() throws CoreException {
        InputStream stream = JdtUtils.createInputStream(element);
        if (stream == null) {
            throw new CoreException(new Status(
                IStatus.ERROR, "de.loskutov.bco", -1,
                "cannot get bytecode from class file", null));
        }
        DecompiledClass decompiledClass = null;
        try {
            decompiledClass = DecompilerHelper.getDecompiledClass(
                stream, new DecompilerOptions(null, methodName, modes, null));
        } catch (IOException e) {
            throw new CoreException(new Status(
                IStatus.ERROR, "de.loskutov.bco", -1,
                "cannot get bytecode dump", e));
        } catch (UnsupportedClassVersionError e){
            throw new CoreException(new Status(
                IStatus.ERROR, "de.loskutov.bco", -1,
                "Error caused by attempt to load class compiled with Java version which"
                + " is not supported by current JVM", e));
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                BytecodeOutlinePlugin.log(e, IStatus.WARNING);
            }
        }
        final byte[] bytes = decompiledClass.getText().getBytes(Charset.forName("UTF-8"));
        // use internal buffering to prevent multiple calls to this method
        Display.getDefault().syncExec(new Runnable(){
            @Override
            public void run() {
                setContent(bytes);
            }
        });

        return new ByteArrayInputStream(bytes);
    }

    /**
     *
     * @param mode one of BCOConstants.F_* modes
     * @param value
     */
    public void setMode(int mode, boolean value){
        modes.set(mode, value);
        // force create new stream
        discardBuffer();
    }
}
