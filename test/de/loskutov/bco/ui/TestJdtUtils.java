package de.loskutov.bco.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public abstract class TestJdtUtils extends TestCase {

    private static final boolean onlyPrintOutError = false;
    private static String EXTERNAL_JAR_DIR_PATH;
    private IWorkspace workspace;
    private IWorkspaceRoot root;
    private IJavaProject project;

    protected abstract String getFieldName() ;

    protected abstract String getJdkVersion();

    protected String getJavaProjectName() {
        return "TestJdtUtils" + getFieldName();
    }

    protected void setUp() throws Exception {
        super.setUp();
        if(project == null){
            workspace = ResourcesPlugin.getWorkspace();
            root = workspace.getRoot();
            project = setUpJavaProject(getJavaProjectName(), getJdkVersion());
            project.getProject().build(
                IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
            project.makeConsistent(null);
        }
    }

    protected void tearDown() throws Exception {
        deleteProject(project.getProject());
        super.tearDown();
    }


    protected IJavaProject getJavaProject() {
        return project;
    }

    protected ICompilationUnit getCompilationUnit(String cuName)
        throws JavaModelException {

        ICompilationUnit compilationUnit = getCompilationUnit("src", "inner", cuName);
        return compilationUnit;
    }

    /**
     * Returns the specified compilation unit in the given project, root, and package
     * fragment or <code>null</code> if it does not exist.
     */
    protected ICompilationUnit getCompilationUnit(String rootPath,
        String packageName, String cuName) throws JavaModelException {
        IPackageFragment pkg = getPackageFragment(rootPath, packageName);
        if (pkg == null) {
            return null;
        }
        return pkg.getCompilationUnit(cuName);
    }

    /**
     * Returns the specified package fragment in the given project and root, or
     * <code>null</code> if it does not exist. The rootPath must be specified as a
     * project relative path. The empty path refers to the default package fragment.
     */
    protected IPackageFragment getPackageFragment(String rootPath,
        String packageName) throws JavaModelException {
        IPackageFragmentRoot root1 = getPackageFragmentRoot(rootPath);
        if (root1 == null) {
            return null;
        }
        return root1.getPackageFragment(packageName);
    }

    /**
     * Returns the specified package fragment root in the given project, or
     * <code>null</code> if it does not exist. If relative, the rootPath must be
     * specified as a project relative path. The empty path refers to the package fragment
     * root that is the project folder iteslf. If absolute, the rootPath refers to either
     * an external jar, or a resource internal to the workspace
     */
    protected IPackageFragmentRoot getPackageFragmentRoot(String rootPath)
        throws JavaModelException {

        IJavaProject project = getJavaProject();
        if (project == null) {
            return null;
        }
        IPath path = new Path(rootPath);
        if (path.isAbsolute()) {
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
                .getRoot();
            IResource resource = workspaceRoot.findMember(path);
            IPackageFragmentRoot root1;
            if (resource == null) {
                // external jar
                root1 = project.getPackageFragmentRoot(rootPath);
            } else {
                // resource in the workspace
                root1 = project.getPackageFragmentRoot(resource);
            }
            return root1;
        }
        IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
        if (roots == null || roots.length == 0) {
            return null;
        }
        for (int i = 0; i < roots.length; i++) {
            IPackageFragmentRoot root1 = roots[i];
            if (!root1.isExternal()
                && root1.getUnderlyingResource().getProjectRelativePath()
                    .equals(path)) {
                return root1;
            }
        }
        return null;
    }


    protected IJavaProject createJavaProject(final String projectName,
        final String[] sourceFolders, final String[] libraries,
        final String projectOutput, final String compliance)
        throws CoreException {
        final IJavaProject[] result = new IJavaProject[1];
        IWorkspaceRunnable create = new IWorkspaceRunnable() {

            public void run(IProgressMonitor monitor) throws CoreException {
                // create project
                createProject(projectName);

                // set java nature
                addJavaNature(projectName);

                // create classpath entries
                IProject project = root.getProject(projectName);
                IPath projectPath = project.getFullPath();
                int sourceLength = sourceFolders.length;
                int libLength = libraries.length;

                IClasspathEntry[] entries = new IClasspathEntry[sourceLength
                    + libLength];
                for (int i = 0; i < sourceLength; i++) {
                    IPath sourcePath = new Path(sourceFolders[i]);
                    int segmentCount = sourcePath.segmentCount();
                    if (segmentCount > 0) {
                        // create folder and its parents
                        IContainer container = project;
                        for (int j = 0; j < segmentCount; j++) {
                            IFolder folder = container.getFolder(new Path(
                                sourcePath.segment(j)));
                            if (!folder.exists()) {
                                folder.create(true, true, null);
                            }
                            container = folder;
                        }
                    }

                    // create source entry
                    entries[i] = JavaCore.newSourceEntry(
                        projectPath.append(sourcePath), new IPath[0],
                        new IPath[0], null);
                }

                for (int i = 0; i < libLength; i++) {
                    String lib = libraries[i];
                    if (lib.startsWith("JCL")) {
                        // ensure JCL variables are set
                         try {
                            setUpJCLClasspathVariables(compliance);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (lib.indexOf(File.separatorChar) == -1
                        && lib.charAt(0) != '/'
                        && lib.equals(lib.toUpperCase())) { // all upper case is a var
                        char[][] vars = CharOperation.splitOn(',', lib
                            .toCharArray());
                        entries[sourceLength + i] = JavaCore.newVariableEntry(
                            new Path(new String(vars[0])), vars.length > 1
                                ? new Path(new String(vars[1]))
                                : null, vars.length > 2
                                ? new Path(new String(vars[2]))
                                : null);
                    } else {
                        IPath libPath = new Path(lib);
                        if (!libPath.isAbsolute() && libPath.segmentCount() > 0
                            && libPath.getFileExtension() == null) {
                            project.getFolder(libPath).create(true, true, null);
                            libPath = projectPath.append(libPath);
                        }
                        entries[sourceLength + i] = JavaCore.newLibraryEntry(
                            libPath, null, null, ClasspathEntry.getAccessRules(
                                new IPath[0], new IPath[0]),
                            new IClasspathAttribute[0], false);
                    }
                }

                // create project's output folder
                IPath outputPath = new Path(projectOutput);
                if (outputPath.segmentCount() > 0) {
                    IFolder output = project.getFolder(outputPath);
                    if (!output.exists()) {
                        output.create(true, true, null);
                    }
                }

                // set classpath and output location
                IJavaProject javaProject = JavaCore.create(project);
                javaProject.setRawClasspath(entries, projectPath
                    .append(outputPath), null);

                // set compliance level options
                if ("1.5".equals(compliance)) {
                    Map<String, String> options = new HashMap<String, String>();
                    options.put(
                        CompilerOptions.OPTION_Compliance,
                        CompilerOptions.VERSION_1_5);
                    options.put(
                        CompilerOptions.OPTION_Source,
                        CompilerOptions.VERSION_1_5);
                    options.put(
                        CompilerOptions.OPTION_TargetPlatform,
                        CompilerOptions.VERSION_1_5);
                    javaProject.setOptions(options);
                }

                result[0] = javaProject;
            }
        };
        workspace.run(create, null);
        return result[0];
    }

    protected IProject createProject(final String projectName)
        throws CoreException {
        final IProject project1 = root.getProject(projectName);

        deleteProject(project1);

        IWorkspaceRunnable create = new IWorkspaceRunnable() {

            public void run(IProgressMonitor monitor) throws CoreException {
                project1.create(null);
                project1.open(null);
            }
        };
        workspace.run(create, null);
        return project1;
    }

    protected void addJavaNature(String projectName) throws CoreException {
        IProject project1 = root.getProject(projectName);
        IProjectDescription description = project1.getDescription();
        description.setNatureIds(new String[]{JavaCore.NATURE_ID});
        project1.setDescription(description, null);
    }

    protected void deleteProject(IProject project1) throws CoreException {
        if (project1.exists() && !project1.isOpen()) { // force opening so that project
                                                        // can be deleted without logging
                                                        // (see bug 23629)
            project1.open(null);
        }
        project1.delete(true, null);
    }


    protected IJavaProject setUpJavaProject(final String projectName,
        String compliance) throws CoreException, IOException {
        // copy files in project from source workspace to target workspace
        String sourceWorkspacePath = getSourcesPath();

        String targetWorkspacePath = root.getLocation().toFile()
            .getCanonicalPath();
        copyDirectory(new File(sourceWorkspacePath), new File(
            targetWorkspacePath + "/" + projectName, "src"));

        // ensure variables are set
        setUpJCLClasspathVariables(compliance);

        String sdkLib = "JCL15_LIB";
        if(!"1.5".equals(compliance)){
            sdkLib = "JCL_LIB";
        }

        // create project
        IJavaProject javaProject = createJavaProject(
            projectName, new String[]{"src"}, new String[] {sdkLib}, "bin", compliance);

        setUpProjectCompliance(javaProject, compliance);
        return javaProject;
    }

    protected void setUpProjectCompliance(IJavaProject javaProject,
        String compliance) throws JavaModelException, IOException {
        // Look for version to set and return if that's already done
        String version = CompilerOptions.VERSION_1_4;
        String jclLibString = null;
        String newJclLibString = null;
        switch (compliance.charAt(2)) {
            case '5' :
                version = CompilerOptions.VERSION_1_5;
                if (version.equals(javaProject.getOption(
                    CompilerOptions.OPTION_Compliance, false))) {
                    return;
                }
                jclLibString = "JCL_LIB";
                newJclLibString = "JCL15_LIB";
                break;
            case '3' :
                version = CompilerOptions.VERSION_1_3;
            default :
                if (version.equals(javaProject.getOption(
                    CompilerOptions.OPTION_Compliance, false))) {
                    return;
                }
                jclLibString = "JCL15_LIB";
                newJclLibString = "JCL_LIB";
                break;
        }

        // ensure variables are set
        setUpJCLClasspathVariables(compliance);

        // set options
        Map<String, String> options = new HashMap<String, String>();
        options.put(CompilerOptions.OPTION_Compliance, version);
        options.put(CompilerOptions.OPTION_Source, version);
        options.put(CompilerOptions.OPTION_TargetPlatform, version);
        javaProject.setOptions(options);

        // replace JCL_LIB with JCL15_LIB, and JCL_SRC with JCL15_SRC
        IClasspathEntry[] classpath = javaProject.getRawClasspath();
        IPath jclLib = new Path(jclLibString);
        for (int i = 0, length = classpath.length; i < length; i++) {
            IClasspathEntry entry = classpath[i];
            if (entry.getPath().equals(jclLib)) {
                classpath[i] = JavaCore.newVariableEntry(
                    new Path(newJclLibString), null, entry
                        .getSourceAttachmentRootPath(), entry.getAccessRules(),
                    new IClasspathAttribute[0], entry.isExported());
                break;
            }
        }
        javaProject.setRawClasspath(classpath, null);
    }

    protected void setUpJCLClasspathVariables(String compliance) throws JavaModelException, IOException {
        if ("1.5".equals(compliance)) {
            if (JavaCore.getClasspathVariable("JCL15_LIB") == null) {
                setupExternalJCL("jclMin1.5");
                JavaCore.setClasspathVariables(
                    new String[] {"JCL15_LIB", "JCL15_SRC", "JCL_SRCROOT"},
                    new IPath[] {getExternalJCLPath(compliance), null, null},
                    null);
            }
        } else {
            if (JavaCore.getClasspathVariable("JCL_LIB") == null) {
                setupExternalJCL("jclMin");
                JavaCore.setClasspathVariables(
                    new String[] {"JCL_LIB", "JCL_SRC", "JCL_SRCROOT"},
                    new IPath[] {getExternalJCLPath(), null, null},
                    null);
            }
        }
    }

    /**
     * Returns the IPath to the external java class library (e.g. jclMin.jar)
     */
    protected IPath getExternalJCLPath() {
        return new Path(getExternalJCLPathString(""));
    }
    /**
     * Returns the IPath to the external java class library (e.g. jclMin.jar)
     */
    protected IPath getExternalJCLPath(String compliance) {
        return new Path(getExternalJCLPathString(compliance));
    }

    /**
     * Returns the java.io path to the external java class library (e.g. jclMin.jar)
     */
    protected String getExternalJCLPathString(String compliance) {
        return getExternalPath() + "jclMin" + compliance + ".jar";
    }

    /**
     * Check locally for the required JCL files, <jclName>.jar and <jclName>src.zip.
     * If not available, copy from the project resources.
     */
    protected void setupExternalJCL(String jclName) throws IOException {
        String externalPath = getExternalPath();
        String separator = java.io.File.separator;
        String resourceJCLDir = getPluginDirectoryPath() + separator + "test" + separator + "JCL";
        java.io.File jclDir = new java.io.File(externalPath);
        java.io.File jclMin =
            new java.io.File(externalPath + jclName + ".jar");
        if (!jclDir.exists()) {
            if (!jclDir.mkdir()) {
                //mkdir failed
                throw new IOException("Could not create the directory " + jclDir);
            }
            //copy the file to the JCL directory
            java.io.File resourceJCLMin =
                new java.io.File(resourceJCLDir + separator + jclName + ".jar");
            copy(resourceJCLMin, jclMin);
        } else {
            //check that the file, jclMin.jar present
            //copy either file that is missing or less recent than the one in workspace
            java.io.File resourceJCLMin =
                new java.io.File(resourceJCLDir + separator + jclName + ".jar");
            if ((jclMin.lastModified() < resourceJCLMin.lastModified())
                    || (jclMin.length() != resourceJCLMin.length())) {
                copy(resourceJCLMin, jclMin);
            }
        }
    }

    /*
     * Returns the OS path to the external directory that contains external jar files.
     * This path ends with a File.separatorChar.
     */
    protected String getExternalPath() {
        if (EXTERNAL_JAR_DIR_PATH == null) {
            try {
                String path = root.getLocation().toFile().getParentFile().getCanonicalPath();
                if (path.charAt(path.length()-1) != File.separatorChar) {
                    path += File.separatorChar;
                }
                EXTERNAL_JAR_DIR_PATH = path;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return EXTERNAL_JAR_DIR_PATH;
    }

    /**
     * Copy the given source directory (and all its contents) to the given target
     * directory.
     */
    protected void copyDirectory(File source, File target) throws IOException {
        if (!target.exists()) {
            boolean result = target.mkdirs();
            if(!result){
                throw new IOException("Can't create directory: " + target);
            }
        }
        File[] files = source.listFiles();
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            File sourceChild = files[i];
            String name = sourceChild.getName();
            if (name.equals("CVS") || name.equals("loskutov")) {
                continue;
            }
            File targetChild = new File(target, name);
            if (sourceChild.isDirectory()) {
                copyDirectory(sourceChild, targetChild);
            } else {
                copy(sourceChild, targetChild);
            }
        }
    }

    /**
     * Copy file from src (path to the original file) to dest (path to the destination
     * file).
     */
    protected void copy(File src, File dest) throws IOException {
        // read source bytes
        byte[] srcBytes = this.read(src);

        // write bytes to dest
        FileOutputStream out = new FileOutputStream(dest);
        out.write(srcBytes);
        out.close();
    }

    protected byte[] read(java.io.File file) throws java.io.IOException {
        int fileLength;
        byte[] fileBytes = new byte[fileLength = (int) file.length()];
        java.io.FileInputStream stream = new java.io.FileInputStream(file);
        int bytesRead = 0;
        int lastReadSize = 0;
        while ((lastReadSize != -1) && (bytesRead != fileLength)) {
            lastReadSize = stream.read(fileBytes, bytesRead, fileLength
                - bytesRead);
            bytesRead += lastReadSize;
        }
        stream.close();
        return fileBytes;
    }

    /**
     * Returns the OS path to the directory that contains this plugin.
     */
    protected String getPluginDirectoryPath() {
        try {
            URL platformURL = Platform.getBundle("de.loskutov.BytecodeOutline")
                .getEntry("/");
            return new File(FileLocator.toFileURL(platformURL).getFile())
                .getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String getSourcesPath() {
        return getPluginDirectoryPath() + java.io.File.separator + "test";
    }

    protected IType[] getAllTypes(ICompilationUnit cu) throws JavaModelException {
        ArrayList<IJavaElement> list = new ArrayList<IJavaElement>();
        collectAllTypes(cu, list);
        return list.toArray(new IType[list.size()]);
    }

    protected void collectAllTypes(IParent parent, List<IJavaElement> types) throws JavaModelException {
        // this call has a (good) side effect that the IParent will be opened
        IJavaElement[] children = parent.getChildren();
        for (int i = 0; i < children.length; i++) {
            if(children[i] instanceof IType){
                types.add(children[i]);
                collectAllTypes((IParent) children[i], types);
            } else if (children[i] instanceof IParent){
                collectAllTypes((IParent) children[i], types);
            }
        }
    }

    protected void doTest(String topClassName) throws JavaModelException {
        System.out.println("Test with " + topClassName + ".java");

        ICompilationUnit cu = getCompilationUnit(topClassName + ".java");
        assertNotNull(cu);

        String packagePath = root.getLocation().append(getJavaProjectName()).append(
            "bin").append("inner").toOSString()
            + File.separator;

        String fieldName = getFieldName();
        IType[] allTypes = getAllTypes(cu);
        for (int i = 0; i < allTypes.length; i++) {
            IType type = allTypes[i];
            IField field = type.getField(fieldName);
            if (field == null) {
                continue;
            }
            String constant = (String) field.getConstant();
            if(constant != null){
                constant = constant.substring(1, constant.length() - 1);
            }
            String expectedPath = packagePath + constant + ".class";
            String name = JdtUtils.getByteCodePath(type);
            if(!(expectedPath).equals(name)){
                System.out.println("Expected/received: \nexpected  -> " + expectedPath + "\nreceived -> " + name + "\n");
                if(!onlyPrintOutError) {
                    assertEquals(expectedPath, name);
                }
            } else {
                System.out.println("OK: " + name);
            }
        }
    }

    protected void pauseTests() throws InterruptedException {
        while (true) {
            synchronized (this) {
                wait(50);
                while (Display.getDefault().readAndDispatch()) {
                    // do events
                }
            }
            if (PlatformUI.getWorkbench().isClosing()) {
                break;
            }
        }
    }
}
