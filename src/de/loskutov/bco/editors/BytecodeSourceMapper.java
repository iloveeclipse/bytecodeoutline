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
import org.eclipse.debug.internal.ui.contexts.DebugContextManager;
import org.eclipse.debug.internal.ui.contexts.provisional.IDebugContextListener;
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
import org.eclipse.ui.IWorkbenchPart;

import de.loskutov.bco.BytecodeOutlinePlugin;
import de.loskutov.bco.asm.DecompiledClass;
import de.loskutov.bco.asm.DecompilerClassVisitor;
import de.loskutov.bco.ui.JdtUtils;

/**
 * @author Jochen Klein
 * @author Andrei
 */
public class BytecodeSourceMapper implements IDebugContextListener {

    /** key is IClassFile, value is DecompiledClass */
    private WeakHashMap classToDecompiled;
    private IJavaReferenceType lastTypeInDebugger;

    public BytecodeSourceMapper() {
        super();
        classToDecompiled = new WeakHashMap();
        DebugContextManager.getDefault().addDebugContextListener(this);
    }

    public char[] getSource(IClassFile classFile, BitSet decompilerFlags) {
        IType type;
        try {
            type = classFile.getType();
        } catch (JavaModelException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            return null;
        }
        if (!type.isBinary()) {
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
        DecompiledClass dc = (DecompiledClass) classToDecompiled.get(cf);
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
        InputStream inputStream = null;
        DecompiledClass decompiledClass = null;
        try {
            ZipFile zf = new ZipFile(archivePath);
            ZipEntry ze = zf.getEntry(className);
            inputStream = zf.getInputStream(ze);
            decompiledClass = decompile(source, inputStream, decompilerFlags);
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
        return decompiledClass;
    }

    private DecompiledClass decompile(StringBuffer source, InputStream is,
        BitSet decompilerFlags) throws IOException {
        DecompiledClass decompiledClass = DecompilerClassVisitor
            .getDecompiledClass(is, null, null, decompilerFlags, null);
        source.append(decompiledClass.getText());
        return decompiledClass;
    }

    private String getArchivePath(IPackageFragmentRoot root) {
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
        DecompiledClass dc = (DecompiledClass) classToDecompiled.get(cf);
        if (dc != null) {
            return dc.getJavaElement(decompiledLine, cf);
        }
        return cf;
    }

    public int mapToSource(int decompiledLine, IClassFile cf) {
        if (cf == null) {
            return 0;
        }
        DecompiledClass dc = (DecompiledClass) classToDecompiled.get(cf);
        if (dc != null) {
            return dc.getSourceLine(decompiledLine);
        }
        return 0;
    }

    public int mapToDecompiled(int sourceLine, IClassFile cf) {
        if (cf == null) {
            return 0;
        }
        DecompiledClass dc = (DecompiledClass) classToDecompiled.get(cf);
        if (dc != null) {
            return dc.getDecompiledLine(sourceLine);
        }
        return 0;
    }

    public void contextActivated(ISelection selection, IWorkbenchPart part) {
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
                } catch (DebugException e) {
                    BytecodeOutlinePlugin.log(e, IStatus.ERROR);
                }
            }
        }
    }

    public void contextChanged(ISelection selection, IWorkbenchPart part) {
        contextActivated(selection, part);
    }

    public IJavaReferenceType getLastTypeInDebugger() {
        return lastTypeInDebugger;
    }

}
