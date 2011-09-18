/*****************************************************************************************
 * Copyright (c) 2011 Andrey Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor: Jochen Klein - initial API and implementation
 * Contributor: Andrey Loskutov - fixes
 ****************************************************************************************/
package de.loskutov.bco.editors;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.WeakHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.debug.core.model.JDIStackFrame;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import de.loskutov.bco.BytecodeOutlinePlugin;
import de.loskutov.bco.asm.DecompiledClass;
import de.loskutov.bco.asm.DecompilerHelper;
import de.loskutov.bco.asm.DecompilerOptions;
import de.loskutov.bco.ui.JdtUtils;

/**
 * @author Jochen Klein
 * @author Andrei
 */
public class BytecodeSourceMapper implements IDebugContextListener {

    /** key is IClassFile, value is DecompiledClass */
    private final WeakHashMap<IClassFile, DecompiledClass> classToDecompiled;
    private IJavaReferenceType lastTypeInDebugger;
    private String lastMethodInDebugger;

    public BytecodeSourceMapper() {
        super();
        classToDecompiled = new WeakHashMap<IClassFile, DecompiledClass>();
        DebugUITools.getDebugContextManager().addDebugContextListener(this);
    }

    public char[] getSource(IClassFile classFile, BitSet decompilerFlags) {
        IType type;
        type = classFile.getType();
        if (type == null || !type.isBinary()) {
            return null;
        }
        IBinaryType info = null;
        try {
            info = (IBinaryType) ((BinaryType) type).getElementInfo();
        } catch (JavaModelException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            return null;
        }
        if (info == null) {
            return null;
        }
        return findSource(type, info, classFile, decompilerFlags);
    }

    public char[] getSource(IFile file, IClassFile cf, BitSet decompilerFlags) {
        StringBuffer source = new StringBuffer();

        DecompiledClass decompiledClass = decompile(source, file.getLocation()
            .toOSString(), decompilerFlags);

        classToDecompiled.put(cf, decompiledClass);

        return source.toString().toCharArray();
    }

    /**
     *
     */
    protected char[] findSource(IType type, IBinaryType info, IClassFile cf,
        BitSet decompilerFlags) {

        IPackageFragment pkgFrag = type.getPackageFragment();
        IPackageFragmentRoot root = (IPackageFragmentRoot) pkgFrag.getParent();
        String pkg = type.getPackageFragment().getElementName().replace(
            '.', '/');

        String classFile = new String(info.getFileName());
        int p = classFile.lastIndexOf('/');
        classFile = classFile.substring(p + 1);

        StringBuffer source = new StringBuffer();
        String location = null;
        String className = pkg + "/" + classFile;
        if (root.isArchive()) {
            location = getArchivePath(root);
            DecompiledClass decompiledClass = decompileFromArchive(
                source, location, className, decompilerFlags);
            classToDecompiled.put(cf, decompiledClass);
        } else {
            try {
                location = root.getUnderlyingResource().getLocation()
                    .toOSString()
                    + "/" + className;
                DecompiledClass decompiledClass = decompile(
                    source, location, decompilerFlags);
                classToDecompiled.put(cf, decompiledClass);
            } catch (JavaModelException e) {
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            }
        }

        source.append("\n\n// DECOMPILED FROM: ");
        source.append(location).append("\n");
        return source.toString().toCharArray();
    }

    public int getDecompiledLine(IMember elt, IClassFile cf) {
        DecompiledClass dc = classToDecompiled.get(cf);
        if (dc != null) {
            String signature = JdtUtils.getMethodSignature(elt);
            if (signature != null) {
                return dc.getDecompiledLine(signature);
            }
        }
        return 0;
    }

    protected DecompiledClass decompile(StringBuffer source, String filePath,
        BitSet decompilerFlags) {
        FileInputStream inputStream = null;
        DecompiledClass dc = null;
        try {
            inputStream = new FileInputStream(filePath);
            dc = decompile(source, inputStream, decompilerFlags);
        } catch (IOException e) {
            source.append(e.toString());
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    BytecodeOutlinePlugin.log(e, IStatus.ERROR);
                }
            }
        }
        return dc;
    }

    protected DecompiledClass decompileFromArchive(StringBuffer source,
        String archivePath, String className, BitSet decompilerFlags) {
        if(archivePath == null){
            return null;
        }
        DecompiledClass decompiledClass = null;
        ZipFile zf = null;
        try {
            zf = new ZipFile(archivePath);
            ZipEntry ze = zf.getEntry(className);
            InputStream inputStream = zf.getInputStream(ze);
            decompiledClass = decompile(source, inputStream, decompilerFlags);
        } catch (IOException e) {
            source.append(e.toString());
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException e) {
                    BytecodeOutlinePlugin.log(e, IStatus.ERROR);
                }
            }
        }
        return decompiledClass;
    }

    private static DecompiledClass decompile(StringBuffer source, InputStream is,
        BitSet decompilerFlags) throws IOException {
        DecompiledClass decompiledClass = DecompilerHelper
            .getDecompiledClass(is, new DecompilerOptions(null, null, decompilerFlags, null));
        source.append(decompiledClass.getText());
        return decompiledClass;
    }

    private static String getArchivePath(IPackageFragmentRoot root) {
        String archivePath = null;
        IResource resource;

        try {
            if ((resource = root.getUnderlyingResource()) != null) {
                // jar in workspace
                archivePath = resource.getLocation().toOSString();
            } else {
                // external jar
                archivePath = root.getPath().toOSString();
            }
        } catch (JavaModelException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        return archivePath;
    }

    protected IJavaElement findElement(IClassFile cf, int decompiledLine) {
        DecompiledClass dc = classToDecompiled.get(cf);
        if (dc != null) {
            return dc.getJavaElement(decompiledLine, cf);
        }
        return cf;
    }

    public int mapToSource(int decompiledLine, IClassFile cf) {
        if (cf == null) {
            return 0;
        }
        DecompiledClass dc = classToDecompiled.get(cf);
        if (dc != null) {
            return dc.getSourceLine(decompiledLine);
        }
        return 0;
    }

    /**
     *
     * @param cf
     * @return mapped class or null
     */
    public DecompiledClass getDecompiledClass(IClassFile cf){
        if(cf == null){
            return null;
        }
        return classToDecompiled.get(cf);
    }

    public int mapToDecompiled(int sourceLine, IClassFile cf) {
        if (cf == null) {
            return 0;
        }
        DecompiledClass dc = classToDecompiled.get(cf);
        if (dc != null) {
            return dc.getDecompiledLine(sourceLine);
        }
        return 0;
    }

    public IJavaReferenceType getLastTypeInDebugger() {
        return lastTypeInDebugger;
    }

    public int mapDebuggerToDecompiled(IClassFile cf) {
        if(cf == null || lastMethodInDebugger == null){
            return -1;
        }
        DecompiledClass dc = classToDecompiled.get(cf);
        if (dc != null) {
            return dc.getDecompiledLine(lastMethodInDebugger);
        }
        return -1;
    }

    @Override
    public void debugContextChanged(DebugContextEvent event) {
        ISelection selection = event.getContext();
        if(selection instanceof IStructuredSelection){
            IStructuredSelection sSelection = (IStructuredSelection) selection;
            if(sSelection.isEmpty()){
                return;
            }
            Object element = sSelection.getFirstElement();
            if(element instanceof JDIStackFrame){
                JDIStackFrame frame = (JDIStackFrame) element;
                try {
                    lastTypeInDebugger = frame.getReferenceType();
                    lastMethodInDebugger = frame.getMethodName() + frame.getSignature();
                } catch (DebugException e) {
                    BytecodeOutlinePlugin.log(e, IStatus.ERROR);
                }
            }
        }
    }

}
