package de.loskutov.bco.editors;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.core.SourceMapper;

import de.loskutov.bco.BytecodeOutlinePlugin;
import de.loskutov.bco.asm.DecompiledClass;
import de.loskutov.bco.asm.DecompilerClassVisitor;

/**
 * Yet "better" way to hook into JDT... using a source mapper enables us to use the
 * outline! Sorry, still ne better news for debuging.
 * @author Jochen Klein
 */
public class BytecodeSourceMapper extends SourceMapper {

    public BytecodeSourceMapper() {
        // values are never used, as we have overwritten
        // findSource()

        // super(new Path("."), "");
        super(new Path("."), "", new HashMap()); // per Rene's e-mail
    }

    /*
     * (non-Javadoc) R2.1 fix
     * @see org.eclipse.jdt.internal.core.SourceMapper#findSource(org.eclipse.jdt.core.IType)
     */
    public char[] findSource(IType type) {
        if (!type.isBinary()) {
            return null;
        }
        BinaryType parent = (BinaryType) type.getDeclaringType();
        BinaryType declType = (BinaryType) type;
        while (parent != null) {
            declType = parent;
            parent = (BinaryType) declType.getDeclaringType();
        }
        IBinaryType info = null;
        try {
            info = (IBinaryType) declType.getElementInfo();
        } catch (JavaModelException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            return null;
        }
        if (info == null) {
            return null;
        }
        return findSource(type, info);
    }

    /**
     */
    public char[] findSource(IType type, IBinaryType info) {
        Collection exceptions = new LinkedList();
        IPackageFragment pkgFrag = type.getPackageFragment();
        IPackageFragmentRoot root = (IPackageFragmentRoot) pkgFrag.getParent();
        String pkg = type.getPackageFragment().getElementName().replace(
            '.', '/');
        String location = "\tDECOMPILED FROM: ";
        String classFile = new String(info.getFileName());
        int p = classFile.lastIndexOf('/');
        classFile = classFile.substring(p + 1);

        StringBuffer source = new StringBuffer();

        if (root.isArchive()) {
            String archivePath = getArchivePath(root);
            location += archivePath;
            decompileFromArchive(source, archivePath, pkg, classFile);
        } else {
            try {
                location += root.getUnderlyingResource().getLocation()
                    .toOSString()
                    + "/" + pkg + "/" + classFile;
                decompile(source, root.getUnderlyingResource().getLocation()
                    .toOSString(), pkg, classFile);
            } catch (JavaModelException e) {
                exceptions.add(e);
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            }
        }

        source.append("\n\n");
        source.append(location).append("\n");
        // source.append(decompiler.getLog());
        // exceptions.addAll(decompiler.getExceptions());
        // logExceptions(exceptions, source);

        return source.toString().toCharArray();
    }

    public void decompile(StringBuffer source, String root, String packege,
        String className) {
        try {
            decompile(source, new FileInputStream(root + "/" + packege + "/"
                + className));

        } catch (IOException e) {
            source.append(e.toString());
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
    }

    public void decompileFromArchive(StringBuffer source, String archivePath,
        String packege, String className) {
        try {
            ZipFile zf = new ZipFile(archivePath);
            // TODO implement better entry resolution (inner classes?)
            ZipEntry ze = zf.getEntry(packege + "/" + className);
            decompile(source, zf.getInputStream(ze));

        } catch (IOException e) {
            source.append(e.toString());
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
    }

    private void decompile(StringBuffer source, InputStream is)
        throws IOException {
        boolean raw = true;
        boolean asmify = false;
        boolean verify = false;
        DecompiledClass decompiledClass = DecompilerClassVisitor
            .getDecompiledClass(is, null, null, raw, asmify, verify, null);
        source.append(decompiledClass.getText());
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
            throw new RuntimeException("Unexpected Java model exception: "
                + e.toString());
        }
        return archivePath;
    }

    /**
     * Finds the deepest <code>IJavaElement</code> in the hierarchy of
     * <code>elt</elt>'s children (including <code>elt</code> itself)
     * which has a source range that encloses <code>position</code>
     * according to <code>mapper</code>.
     *
     * Code mostly taken from 'org.eclipse.jdt.internal.core.ClassFile'
     */
    protected IJavaElement findElement(IJavaElement elt, int position) {
        ISourceRange range = getSourceRange(elt);
        if (range == null || position < range.getOffset()
            || range.getOffset() + range.getLength() - 1 < position) {
            return null;
        }
        if (elt instanceof IParent) {
            try {
                IJavaElement[] children = ((IParent) elt).getChildren();
                for (int i = 0; i < children.length; i++) {
                    IJavaElement match = findElement(children[i], position);
                    if (match != null) {
                        return match;
                    }
                }
            } catch (JavaModelException e) {
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            }
        }
        return elt;
    }

    /**
     * @see org.eclipse.jdt.internal.core.SourceMapper#mapSource(IType, char[])
     */
    public void mapSource(IType type, char[] contents, boolean force) {
        if (force) {
            fSourceRanges.remove(type);
        }
        super.mapSource(type, contents);
    }

}
