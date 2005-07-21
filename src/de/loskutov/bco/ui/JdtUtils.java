/*****************************************************************************************
 * Copyright (c) 2004 Andrei Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD License which
 * accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php Contributor: Andrei Loskutov -
 * initial API and implementation
 ****************************************************************************************/
package de.loskutov.bco.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.core.BinaryMember;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.eclipse.jdt.internal.core.SourceRange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jface.text.ITextSelection;

import de.loskutov.bco.BytecodeOutlinePlugin;

/**
 * @author Andrei
 */
public class JdtUtils {

    /**
     *
     */
    private JdtUtils() {
        // don't call
    }

    public static String createMethodSignature(IMethod iMethod)
        throws JavaModelException {
        StringBuffer sb = new StringBuffer();

        // Eclipse put class name as constructor name - we change it!
        if (iMethod.isConstructor()) {
            sb.append("<init>"); //$NON-NLS-1$
        } else {
            sb.append(iMethod.getElementName());
        }

        if (iMethod instanceof BinaryMember) {
            // binary info should be full qualified
            return sb.append(iMethod.getSignature()).toString();
        }

        sb.append('(');
        IType declaringType = iMethod.getDeclaringType();
        String[] parameterTypes = iMethod.getParameterTypes();

        // doSomething(Lgenerics/DummyForAsmGenerics;)Lgenerics/DummyForAsmGenerics;
        for (int i = 0; i < parameterTypes.length; i++) {
            String resolvedType = getResolvedType(parameterTypes[i], declaringType);
            if(resolvedType != null && resolvedType.length() > 0){
                sb.append(resolvedType);
            } else {
                // this is a generic type
                appendGenericType(sb, iMethod, parameterTypes[i]);
            }
        }
        sb.append(')');

        // continue here with adding resolved return type
        String returnType = iMethod.getReturnType();
        String resolvedType = getResolvedType(returnType, declaringType);
        if(resolvedType != null && resolvedType.length() > 0){
            sb.append(getResolvedType(returnType, declaringType));
        } else {
            // this is a generic type
            appendGenericType(sb, iMethod, returnType);
        }

        return sb.toString();
    }

    private static void appendGenericType(StringBuffer sb, IMethod iMethod,
        String unresolvedType) throws JavaModelException{
        IType declaringType = iMethod.getDeclaringType();

        // unresolvedType is here like "QA;" => we remove "Q" and ";"
        if(unresolvedType.length() < 3){
            // ???? something wrong here ....
            sb.append(unresolvedType);
            return;
        }
        unresolvedType = unresolvedType.substring(1, unresolvedType.length() - 1);

        ITypeParameter typeParameter = iMethod.getTypeParameter(unresolvedType);
        if(typeParameter == null || !typeParameter.exists()){
            typeParameter = declaringType.getTypeParameter(unresolvedType);
        }

        String[] bounds = typeParameter.getBounds();
        if(bounds.length == 0){
            sb.append("Ljava/lang/Object;");
        } else {
            for (int i = 0; i < bounds.length; i++) {
                String simplyName = bounds[i];
                simplyName =  Signature.C_UNRESOLVED + simplyName + Signature.C_NAME_END;
                String resolvedType = getResolvedType(simplyName, declaringType);
                sb.append(resolvedType);
            }
        }
    }

    /**
     * @param typeToResolve
     * @param declaringType
     * @return full qualified "bytecode formatted" type
     * @throws JavaModelException
     */
    private static String getResolvedType(String typeToResolve,
        IType declaringType) throws JavaModelException {
        StringBuffer sb = new StringBuffer();
        int arrayCount = Signature.getArrayCount(typeToResolve);
        // test which letter is following - Q or L are for reference types
        boolean isPrimitive = isPrimitiveType(typeToResolve.charAt(arrayCount));
        if (isPrimitive) {
            // simply add whole string (probably with array chars like [[I etc.)
            sb.append(typeToResolve);
        } else {
            // we need resolved types
            String resolved = getResolvedTypeName(typeToResolve, declaringType);
            if(resolved != null) {
                while (arrayCount > 0) {
                    sb.append(Signature.C_ARRAY);
                    arrayCount--;
                }
                sb.append(Signature.C_RESOLVED);
                sb.append(resolved);
                sb.append(Signature.C_SEMICOLON);
            }
        }
        return sb.toString();
    }

    /**
     * Copied and modified from JavaModelUtil. Resolves a type name in the context of the
     * declaring type.
     * @param refTypeSig the type name in signature notation (for example 'QVector') this
     * can also be an array type, but dimensions will be ignored.
     * @param declaringType the context for resolving (type where the reference was made
     * in)
     * @return returns the fully qualified <b>bytecode </b> type name or build-in-type
     * name. if a unresoved type couldn't be resolved null is returned
     */
    private static String getResolvedTypeName(String refTypeSig,
        IType declaringType) throws JavaModelException {

        /* the whole method is copied from JavaModelUtil.getResolvedTypeName(...).
         * The problem is, that JavaModelUtil uses '.' to separate package
         * names, but we need '/' -> see JavaModelUtil.concatenateName() vs
         * JdtUtils.concatenateName()
         */
        int arrayCount = Signature.getArrayCount(refTypeSig);
        char type= refTypeSig.charAt(arrayCount);
        if (type == Signature.C_UNRESOLVED) {
            String name= ""; //$NON-NLS-1$
            int bracket= refTypeSig.indexOf(Signature.C_GENERIC_START, arrayCount + 1);
            if (bracket > 0) {
                name= refTypeSig.substring(arrayCount + 1, bracket);
            } else {
                int semi= refTypeSig.indexOf(Signature.C_SEMICOLON, arrayCount + 1);
                if (semi == -1) {
                    throw new IllegalArgumentException();
                }
                name= refTypeSig.substring(arrayCount + 1, semi);
            }
            String[][] resolvedNames= declaringType.resolveType(name);
            if (resolvedNames != null && resolvedNames.length > 0) {
                return concatenateName(resolvedNames[0][0], resolvedNames[0][1]);
            }
            return null;
        }
        return Signature.toString(refTypeSig.substring(arrayCount));
    }

    /**
     * Concatenates package and Class name Both strings can be empty or <code>null</code>.
     */
    private static String concatenateName(String packageName, String className) {
        StringBuffer buf = new StringBuffer();
        if (packageName != null && packageName.length() > 0) {
            packageName = packageName.replace(Signature.C_DOT, '/');
            buf.append(packageName);
        }
        if (className != null && className.length() > 0) {
            if (buf.length() > 0) {
                buf.append('/');
            }
            className = className.replace(Signature.C_DOT, '$');
            buf.append(className);
        }
        return buf.toString();
    }

    /**
     * Test which letter is following - Q or L are for reference types
     * @param first
     * @return true, if character is not a simbol for reference types
     */
    private static boolean isPrimitiveType(char first) {
        return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED);
    }

    /**
     * @param childEl may be null
     * @return first ancestor with IJavaElement.TYPE element type, or null
     */
    public static IType getEnclosingType(IJavaElement childEl) {
        if (childEl == null) {
            return null;
        }
        return (IType) childEl.getAncestor(IJavaElement.TYPE);
    }

    /**
     * Modified copy from org.eclipse.jdt.internal.ui.actions.SelectionConverter
     * @param input
     * @param selection
     * @return
     * @throws JavaModelException
     */
    public static IJavaElement getElementAtOffset(IJavaElement input,
        ITextSelection selection) throws JavaModelException {
        ICompilationUnit workingCopy = null;
        boolean fakedCU = false;
        if (input instanceof ICompilationUnit) {
            workingCopy = (ICompilationUnit) input;
        } else if (input instanceof IClassFile) {
            IClassFile iClass = (IClassFile) input;
            // iClass.gets
            IJavaElement ref = getElementAt(selection.getOffset(), iClass);
            if (ref != null) {
                return ref;
            }
            // ???
            return input;
            // if source offset is over given class file (e.g. another class in
            // same source file, then we try to made a new compilation unit)
            // workingCopy = iClass.getWorkingCopy(
            // (WorkingCopyOwner) null, (IProgressMonitor) null);
            // fakedCU = true;
        }
        if (!fakedCU) {
            JavaModelUtil.reconcile(workingCopy);
        }
        IJavaElement ref = workingCopy.getElementAt(selection.getOffset());
        if (ref == null) {
            return input;
        }
        return ref;
    }

    /**
     * Copied from ClassFile - for usage only with external class files
     * @param position
     * @param iClass
     * @return
     * @throws JavaModelException
     */
    protected static IJavaElement getElementAt(int position, IClassFile iClass)
        throws JavaModelException {
        SourceMapper mapper = getSourceMapper(iClass);
        if (mapper == null) {
            return null;
        }
        // ensure this class file's buffer is open so that source ranges are computed
        iClass.getBuffer();

        IType type = iClass.getType();
        IJavaElement javaElt = findElement(type, position, mapper);
        if (javaElt == null) {
            javaElt = type;
        }
        return javaElt;
    }

    private static SourceMapper getSourceMapper(IClassFile iClass) {
        IJavaElement parentElement = iClass.getParent();
        while (parentElement.getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT) {
            parentElement = parentElement.getParent();
        }
        PackageFragmentRoot root = (PackageFragmentRoot) parentElement;
        return root.getSourceMapper();
    }

    /**
     * Copied from ClassFile - for usage only with external class files
     * @param elt
     * @param position
     * @param mapper
     * @return element
     */
    protected static IJavaElement findElement(IJavaElement elt, int position,
        SourceMapper mapper) {
        SourceRange range = mapper.getSourceRange(elt);
        if (range == null || position < range.getOffset()
            || range.getOffset() + range.getLength() - 1 < position) {
            return null;
        }
        if (elt instanceof IParent) {
            try {
                IJavaElement[] children = ((IParent) elt).getChildren();
                for (int i = 0; i < children.length; i++) {
                    IJavaElement match = findElement(
                        children[i], position, mapper);
                    if (match != null) {
                        return match;
                    }
                }
            } catch (JavaModelException npe) {
                // elt doesn't exist: return the element
            }
        }
        return elt;
    }

    /**
     * Cite: jdk1.1.8/docs/guide/innerclasses/spec/innerclasses.doc10.html: For the sake
     * of tools, there are some additional requirements on the naming of an inaccessible
     * class N. Its bytecode name must consist of the bytecode name of an enclosing class
     * (the immediately enclosing class, if it is a member), followed either by `$' and a
     * positive decimal numeral chosen by the compiler, or by `$' and the simple name of
     * N, or else by both (in that order). Moreover, the bytecode name of a block-local N
     * must consist of its enclosing package member T, the characters `$1$', and N, if the
     * resulting name would be unique.
     * @param javaElement
     * @return simply element name
     */
    public static String getElementName(IJavaElement javaElement) {
        if (isAnonymousType(javaElement)) {
            List allAnonymous = new ArrayList();
            collectAllAnonymous(allAnonymous, (IType) getLastAncestor(
                javaElement, IJavaElement.TYPE));
            int idx = getAnonimousIndex(
                (IMember) javaElement, (IMember[]) allAnonymous
                    .toArray(new IMember[allAnonymous.size()]));
            return Integer.toString(idx);
        }
        String name = javaElement.getElementName();
        if (isInnerFromBlock(javaElement)) {
            name = "1$" + name; // see method comment //$NON-NLS-1$
        }

        if (name.endsWith(".java")) { //$NON-NLS-1$
            name = name.substring(0, name.lastIndexOf(".java")); //$NON-NLS-1$
        } else if (name.endsWith(".class")) { //$NON-NLS-1$
            name = name.substring(0, name.lastIndexOf(".class")); //$NON-NLS-1$
        }
        return name;
    }

    /**
     * @param javaElement
     * @return null, if javaElement is top level class
     */
    static IJavaElement getFirstAncestor(IJavaElement javaElement) {
        IJavaElement parent = javaElement;
        if (javaElement.getElementType() == IJavaElement.TYPE) {
            parent = javaElement.getParent();
        }
        if (parent != null) {
            return parent.getAncestor(IJavaElement.TYPE);
        }
        return null;
    }

    static IJavaElement getLastAncestor(IJavaElement javaElement,
        int elementType) {
        IJavaElement lastFound = null;
        if (elementType == javaElement.getElementType()) {
            lastFound = javaElement;
        }
        IJavaElement parent = javaElement.getParent();
        if (parent == null) {
            return lastFound;
        }
        IJavaElement ancestor = parent.getAncestor(elementType);
        if (ancestor != null) {
            return getLastAncestor(ancestor, elementType);
        }
        return lastFound;
    }

    /**
     * @param javaElement
     * @return distance to given ancestor, 0 if it is the same, -1 if ancestor with type
     * IJavaElement.TYPE does not exist
     */
    static int getTopAncestorDistance(IJavaElement javaElement,
        IJavaElement topAncestor) {
        if (topAncestor == javaElement) {
            return 0;
        }
        IJavaElement ancestor = getFirstAncestor(javaElement);
        if (ancestor != null) {
            return 1 + getTopAncestorDistance(ancestor, topAncestor);
        }
        // this is not possible, if ancestor exist - which return value we should use?
        return -1;
    }

    /**
     * @param javaElement
     * @return true, if given element is anonymous inner class
     */
    private static boolean isAnonymousType(IJavaElement javaElement) {
        return javaElement instanceof IType
            && "".equals(javaElement.getElementName()); //$NON-NLS-1$
    }

    /**
     * @param javaElement
     * @return true, if given element is inner class from initializer block
     */
    private static boolean isInnerFromBlock(IJavaElement javaElement) {
        IJavaElement parent = javaElement.getParent();
        return javaElement instanceof IType
            && (parent != null && parent.getElementType() == IJavaElement.INITIALIZER);
    }

    /**
     * @param javaElement
     * @return absolute path of generated bytecode package for given element
     * @throws JavaModelException
     */
    private static String getPackageOutputPath(IJavaElement javaElement)
        throws JavaModelException {
        String dir = ""; //$NON-NLS-1$
        if (javaElement == null) {
            return dir;
        }

        IJavaProject project = javaElement.getJavaProject();

        if (project == null) {
            return dir;
        }
        // default bytecode location
        IPath path = project.getOutputLocation();

        IResource resource = javaElement.getUnderlyingResource();
        if (resource == null) {
            return dir;
        }
        // resolve multiple output locations here
        if (project.exists() && project.getProject().isOpen()) {
            IClasspathEntry entries[] = project.getRawClasspath();
            for (int i = 0; i < entries.length; i++) {
                IClasspathEntry classpathEntry = entries[i];
                if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPath outputPath = classpathEntry.getOutputLocation();
                    if (outputPath != null
                        && classpathEntry.getPath().isPrefixOf(
                            resource.getFullPath())) {
                        path = outputPath;
                        break;
                    }
                }
            }
        }

        if (path == null) {
            return dir;
        }

        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        if (!project.getPath().equals(path)) {
            IFolder outputFolder = workspace.getRoot().getFolder(path);
            if (outputFolder != null) {
                // linked resources will be resolved here!
                IPath rawPath = outputFolder.getRawLocation();
                if (rawPath != null) {
                    path = rawPath;
                }
            }
        } else {
            path = project.getProject().getLocation();
        }

        // here we should resolve path variables,
        // probably existing at first place of path
        IPathVariableManager pathManager = workspace.getPathVariableManager();
        path = pathManager.resolvePath(path);

        if (path == null) {
            return dir;
        }

        if (isPackageRoot(project, resource)) {
            dir = path.toOSString();
        } else {
            String packPath = EclipseUtils.getJavaPackageName(javaElement)
                .replace('.', '/');
            dir = path.append(packPath).toOSString();
        }
        return dir;
    }

    /**
     * @param project
     * @param pack
     * @return true if 'pack' argument is package root
     * @throws JavaModelException
     */
    private static boolean isPackageRoot(IJavaProject project, IResource pack)
        throws JavaModelException {
        boolean isRoot = false;
        if (project == null || pack == null) {
            return isRoot;
        }
        IPackageFragmentRoot root = project.getPackageFragmentRoot(pack);
        IClasspathEntry clPathEntry = null;
        if (root != null) {
            clPathEntry = root.getRawClasspathEntry();
        }
        isRoot = clPathEntry != null;
        return isRoot;
    }

    /**
     * Works only for eclipse - managed/generated bytecode, ergo not with imported
     * classes/jars
     * @param javaElement
     * @return full os-specific file path to .class resource, containing given element
     */
    public static String getByteCodePath(IJavaElement javaElement) {
        if (javaElement == null) {
            return "";//$NON-NLS-1$
        }
        String packagePath = ""; //$NON-NLS-1$
        try {
            packagePath = getPackageOutputPath(javaElement);
        } catch (JavaModelException e) {
            BytecodeOutlinePlugin.error(null, e);
        }
        IJavaElement ancestor = getLastAncestor(javaElement, IJavaElement.TYPE);
        StringBuffer sb = new StringBuffer(packagePath);
        sb.append(File.separator);
        sb.append(getClassName(javaElement, ancestor));
        sb.append(".class"); //$NON-NLS-1$
        return sb.toString();
    }

    /**
     * @param javaElement
     * @return new generated input stream for gicen element bytecode class file, or null
     * if class file cannot be found or this element is not from java source path
     */
    public static InputStream createInputStream(IJavaElement javaElement) {
        IClassFile classFile = (IClassFile) javaElement
            .getAncestor(IJavaElement.CLASS_FILE);
        InputStream is = null;

        // existing read-only class files
        if (classFile != null) {
            JavaElement jarParent = (JavaElement) classFile.getParent();
            // TODO dirty hack to be sure, that package is from jar -
            // because JarPackageFragment is not public class, we cannot
            // use instanceof here
            boolean isJar = jarParent != null
                && jarParent.getClass().getName()
                    .endsWith("JarPackageFragment"); //$NON-NLS-1$
            if (isJar) {
                is = createStreamFromJar(classFile);
            } else {
                is = createStreamFromClass(classFile);
            }
        } else {
            // usual eclipse - generated bytecode

            boolean inJavaPath = isOnClasspath(javaElement);
            if (!inJavaPath) {
                return null;
            }
            String classPath = getByteCodePath(javaElement);

            try {
                is = new FileInputStream(classPath);
            } catch (FileNotFoundException e) {
                // if autobuild is disabled, we get tons of this errors.
                // but I think we cannot ignore them, therefore WARNING and not
                // ERROR status
                BytecodeOutlinePlugin.log(e, IStatus.WARNING);
            }
        }
        return is;
    }

    /**
     * Creates stream from external class file from Eclipse classpath (means, that this
     * class file is read-only)
     * @param classFile
     * @return new generated input stream from external class file, or null, if class file
     * for this element cannot be found
     */
    private static InputStream createStreamFromClass(IClassFile classFile) {
        IResource underlyingResource = null;
        try {
            // to tell the truth, I don't know why that different methods
            // are not working in a particular case. But it seems to be better
            // to use getResource() with non-java elements (not in model)
            // and getUnderlyingResource() with java elements.
            if (classFile.exists()) {
                underlyingResource = classFile.getUnderlyingResource();
            } else {
                // this is a class file that is not in java model
                underlyingResource = classFile.getResource();
            }
        } catch (JavaModelException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            return null;
        }
        IPath rawLocation = underlyingResource.getRawLocation();
        // here we should resolve path variables,
        // probably existing at first place of "rawLocation" path
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPathVariableManager pathManager = workspace.getPathVariableManager();
        rawLocation = pathManager.resolvePath(rawLocation);
        try {
            return new FileInputStream(rawLocation.toOSString());
        } catch (FileNotFoundException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        return null;
    }

    /**
     * Creates stream from external class file that is stored in jar file
     * @param classFile
     * @param javaElement
     * @return new generated input stream from external class file that is stored in jar
     * file, or null, if class file for this element cannot be found
     */
    private static InputStream createStreamFromJar(IClassFile classFile) {
        IPath path = null;
        IResource resource = classFile.getResource();
        // resource == null => this is a external archive
        if (resource != null) {
            path = resource.getRawLocation();
        } else {
            path = classFile.getPath();
        }
        if (path == null) {
            return null;
        }
        // here we should resolve path variables,
        // probably existing at first place of path
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPathVariableManager pathManager = workspace.getPathVariableManager();
        path = pathManager.resolvePath(path);

        JarFile jar = null;
        try {
            jar = new JarFile(path.toOSString());
        } catch (IOException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            return null;
        }
        String fullClassName = getFullBytecodeName(classFile);
        if (fullClassName == null) {
            return null;
        }
        JarEntry jarEntry = jar.getJarEntry(fullClassName);
        if (jarEntry != null) {
            try {
                return jar.getInputStream(jarEntry);
            } catch (IOException e) {
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            }
        }
        return null;
    }

    private static boolean isOnClasspath(IJavaElement javaElement) {
        IJavaProject project = javaElement.getJavaProject();
        if (project != null) {
            boolean result = project.isOnClasspath(javaElement);
            return result;
        }
        return false;
    }

    /**
     * @param classFile
     * @return full qualified bytecode name of given class
     */
    public static String getFullBytecodeName(IClassFile classFile) {
        IPackageFragment packageFr = (IPackageFragment) classFile
            .getAncestor(IJavaElement.PACKAGE_FRAGMENT);
        if (packageFr == null) {
            return null;
        }
        String packageName = packageFr.getElementName();
        // switch to java bytecode naming conventions
        packageName = packageName.replace('.', '/');

        String className = classFile.getElementName();
        // className = className.replace('.', '$');
        if (packageName != null && packageName.length() > 0) {
            return packageName + '/' + className;
        }
        return className;
    }

    /**
     * @param javaElement
     * @param topAncestor
     * @param sb
     */
    private static String getClassName(IJavaElement javaElement,
        IJavaElement topAncestor) {
        char innerClassSeparator = '$';
        StringBuffer sb = new StringBuffer();
        if (!javaElement.equals(topAncestor)) {
            if (isAnonymousType(javaElement)) {
                sb.append(getElementName(topAncestor));
                sb.append(innerClassSeparator);
            } else {
                // override top ancestor with immediate ancestor
                topAncestor = getFirstAncestor(javaElement);
                while (topAncestor != null) {
                    sb.insert(0, getElementName(topAncestor)
                        + innerClassSeparator);
                    topAncestor = getFirstAncestor(topAncestor);
                }
            }
        }
        sb.append(getElementName(javaElement));
        return sb.toString();
    }

    /**
     * @param list all anonymous classes from given element will be stored in this list,
     * elements instanceof IJavaElement
     * @param javaElement
     */
    private static void collectAllAnonymous(List list, IParent topElement) {
        try {
            IJavaElement[] children = topElement.getChildren();
            for (int i = 0; i < children.length; i++) {
                if (isAnonymousType(children[i])) {
                    list.add(children[i]);
                }
                if (children[i] instanceof IParent) {
                    collectAllAnonymous(list, (IParent) children[i]);
                }
            }
        } catch (JavaModelException e) {
            BytecodeOutlinePlugin.error(null, e);
        }
    }

    private static int getAnonimousIndex(IMember javaElement,
        IMember[] anonymous) {
        sortAnonymous(anonymous, javaElement);
        for (int i = 0; i < anonymous.length; i++) {
            if (anonymous[i] == javaElement) {
                // +1 because compiler starts generated classes always with 1
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Sort given anonymous classes in order like java compiler would generate output
     * classes
     * @param anonymous
     */
    private static void sortAnonymous(IMember[] anonymous, IMember javaElement) {
        SourceOffsetComparator sourceComparator = new SourceOffsetComparator();
        Arrays.sort(anonymous, new AnonymClassComparator(
            javaElement, sourceComparator));
    }

    /**
     * 1) from instance init 2) from deepest inner from instance init (deepest first) 3)
     * from static init 4) from deepest inner from static init (deepest first) 5) from
     * deepest inner (deepest first) 6) regular anon classes from main class
     * @param javaElement
     * @return priority - lesser mean wil be compiled later, a value > 0
     * @throws JavaModelException
     */
    static int getAnonCompilePriority(IMember javaElement,
        IJavaElement firstAncestor, IJavaElement topAncestor) {
        // search for initializer block
        IJavaElement lastAncestor = getLastAncestor(
            javaElement, IJavaElement.INITIALIZER);
        // test is for anon. classes from initializer blocks
        if (lastAncestor != null) {
            IInitializer init = (IInitializer) lastAncestor;
            int flags = 0;
            try {
                flags = init.getFlags();
            } catch (JavaModelException e) {
                BytecodeOutlinePlugin.error(null, e);
            }
            if (!Flags.isStatic(flags)) {
                if (firstAncestor == topAncestor) {
                    return 10; // instance init
                }
                return 9; // from inner from instance init
            }
            if (firstAncestor == topAncestor) {
                return 8; // class init
            }
            return 7; // from inner from class init
        }
        // test for anon. classes from "regular" code
        lastAncestor = getLastAncestor(javaElement, IJavaElement.TYPE);
        if (firstAncestor == topAncestor) {
            return 5; // regular anonyme classes
        }
        return 6; // from inner from main type
    }

    /**
     * @param type
     * @return
     */
    public static ClassLoader getClassLoader(IJavaElement type) {
        ClassLoader cl;

        IJavaProject javaProject = type.getJavaProject();
        List urls = new ArrayList();

        getClassURLs(javaProject, urls);

        if (urls.isEmpty()) {
            cl = JdtUtils.class.getClassLoader();
        } else {
            cl = new URLClassLoader((URL[]) urls.toArray(new URL[urls.size()]));
        }
        return cl;
    }

    private static void getClassURLs(IJavaProject javaProject, List urls) {
        IProject project = javaProject.getProject();
        IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();

        IPath projectPath = javaProject.getProject().getLocation();
        IClasspathEntry[] paths = null;

        IPath defaultOutputLocation = null;

        try {
            paths = javaProject.getResolvedClasspath(true);
            defaultOutputLocation = javaProject.getOutputLocation();
        } catch (JavaModelException e) {
            // don't show message to user
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        if (paths != null) {
            for (int i = 0; i < paths.length; ++i) {
                IClasspathEntry cpEntry = paths[i];
                IPath p = null;
                if (cpEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    // filter out source container - there are unused for class
                    // search -
                    // add bytecode output location instead
                    p = cpEntry.getOutputLocation();
                    if (p == null) {
                        // default output used:
                        p = defaultOutputLocation;
                    }
                } else if (cpEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                    String projName = cpEntry.getPath().toPortableString()
                        .substring(1);
                    IProject proj = workspaceRoot.getProject(projName);
                    IJavaProject projj = JavaCore.create(proj);
                    getClassURLs(projj, urls);
                    continue;
                } else {
                    p = cpEntry.getPath();
                }

                if (p == null) {
                    continue;
                }
                if (!p.toFile().exists()) {
                    // removeFirstSegments: remove project from relative path
                    p = projectPath.append(p.removeFirstSegments(1));
                    if (!p.toFile().exists()) {
                        continue;
                    }
                }
                try {
                    urls.add(p.toFile().toURL());
                } catch (MalformedURLException e) {
                    // don't show message to user
                    BytecodeOutlinePlugin.log(e, IStatus.ERROR);
                }
            }
        }
    }

    /**
     * Check if java element is an interface or abstract method or a method from
     * interface.
     */
    public static boolean isAbstractOrInterface(IJavaElement javaEl) {
        if (javaEl == null) {
            return true;
        }
        boolean abstractOrInterface = false;
        try {
            switch (javaEl.getElementType()) {
                case IJavaElement.CLASS_FILE :
                    abstractOrInterface = ((IClassFile) javaEl).isInterface();
                    break;
                case IJavaElement.COMPILATION_UNIT :
                    ICompilationUnit cUnit = (ICompilationUnit) javaEl;
                    IType type = cUnit.findPrimaryType();
                    abstractOrInterface = type != null && type.isInterface();
                    break;
                case IJavaElement.TYPE :
                    abstractOrInterface = ((IType) javaEl).isInterface();
                    break;
                case IJavaElement.METHOD :
                    // test for "abstract" flag on method in a class
                    abstractOrInterface = Flags.isAbstract(((IMethod) javaEl)
                        .getFlags());
                    // "abstract" flags could be not exist on interface methods
                    if (!abstractOrInterface) {
                        IType ancestor = (IType) javaEl
                            .getAncestor(IJavaElement.TYPE);
                        abstractOrInterface = ancestor != null
                            && ancestor.isInterface();
                    }
                    break;
                default :
                    IType ancestor1 = (IType) javaEl
                        .getAncestor(IJavaElement.TYPE);
                    abstractOrInterface = ancestor1 != null
                        && ancestor1.isInterface();
                    break;
            }
        } catch (JavaModelException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        return abstractOrInterface;
    }

    static class SourceOffsetComparator implements Comparator {

        /**
         * First source occurence win.
         * @param o1 should be IMember
         * @param o2 should be IMember
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2) {
            IMember m1 = (IMember) o1;
            IMember m2 = (IMember) o2;
            int idx1, idx2;
            try {
                ISourceRange sr1 = m1.getSourceRange();
                ISourceRange sr2 = m2.getSourceRange();
                if (sr1 == null || sr2 == null) {
                    return 0;
                }
                idx1 = sr1.getOffset();
                idx2 = sr2.getOffset();
            } catch (JavaModelException e) {
                BytecodeOutlinePlugin.error(null, e);
                return 0;
            }
            if (idx1 < idx2) {
                return -1;
            } else if (idx1 > idx2) {
                return 1;
            }
            return 0;
        }
    }

    static class AnonymClassComparator implements Comparator {

        private IType topAncestorType;
        private SourceOffsetComparator sourceComparator;

        /**
         * @param javaElement
         * @param sourceComparator
         */
        public AnonymClassComparator(IMember javaElement,
            SourceOffsetComparator sourceComparator) {
            this.sourceComparator = sourceComparator;
            topAncestorType = (IType) getLastAncestor(
                javaElement, IJavaElement.TYPE);
        }

        /**
         * If "deep" is the same, then source order win. 1) from instance init 2) from
         * deepest inner from instance init (deepest first) 3) from static init 4) from
         * deepest inner from static init (deepest first) 5) from deepest inner (deepest
         * first) 7) regular anon classes from main class
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2) {
            IMember m1 = (IMember) o1;
            IMember m2 = (IMember) o2;
            IJavaElement firstAncestor1 = getFirstAncestor(m1);
            IJavaElement firstAncestor2 = getFirstAncestor(m2);
            // both have the same ancestor as immediate ancestor
            if (firstAncestor1 == firstAncestor2) {
                return sourceComparator.compare(o1, o2);
            }
            int compilePrio1 = getAnonCompilePriority(
                m1, firstAncestor1, topAncestorType);
            int compilePrio2 = getAnonCompilePriority(
                m2, firstAncestor2, topAncestorType);

            if (compilePrio1 > compilePrio2) {
                return -1;
            } else if (compilePrio1 < compilePrio2) {
                return 1;
            } else {
                int topAncestorDistance1 = getTopAncestorDistance(
                    m1, topAncestorType);
                int topAncestorDistance2 = getTopAncestorDistance(
                    m2, topAncestorType);
                if (topAncestorDistance1 > topAncestorDistance2) {
                    return -1;
                } else if (topAncestorDistance1 < topAncestorDistance2) {
                    return 1;
                } else {
                    return sourceComparator.compare(o1, o2);
                }
            }
        }
    }
}