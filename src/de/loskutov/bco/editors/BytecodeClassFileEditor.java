/* $Id: BytecodeClassFileEditor.java,v 1.9 2008-07-02 19:18:35 andrei Exp $ */

package de.loskutov.bco.editors;

import java.lang.reflect.Constructor;
import java.util.BitSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferChangedListener;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.jdt.internal.ui.javaeditor.ExternalClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.InternalClassFileEditorInput;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import de.loskutov.bco.BytecodeOutlinePlugin;
import de.loskutov.bco.asm.DecompiledClass;
import de.loskutov.bco.preferences.BCOConstants;
import de.loskutov.bco.ui.JdtUtils;

/**
 * A "better" way to hook into JDT...
 * @author Eugene Kuleshov, V. Grishchenko, Jochen Klein, Andrei Loskutov
 */
public class BytecodeClassFileEditor extends ClassFileEditor {

    private final InputUpdater fInputUpdater;
    public static final String ID = "de.loskutov.bco.editors.BytecodeClassFileEditor";
    public static final String MARK = "// class version ";
    /** the modes (flags) for the decompiler */
    private final BitSet decompilerFlags;
    /** is not null only on class files with decompiled source */
    private static BytecodeSourceMapper sourceMapper;
    private BytecodeDocumentProvider fClassFileDocumentProvider;
    private boolean hasMappedSource;
    private boolean decompiled;
    private boolean initDone;
    private boolean sourceAttachmentPossible;

    /**
     * Constructor for JadclipseClassFileEditor.
     */
    public BytecodeClassFileEditor() {
        super();
        if (sourceMapper == null) {
            sourceMapper = new BytecodeSourceMapper();
        }
        fInputUpdater = new InputUpdater();
        setDocumentProvider(getClassFileDocumentProvider());
        setEditorContextMenuId("#ClassFileEditorContext"); //$NON-NLS-1$
        setRulerContextMenuId("#ClassFileRulerContext"); //$NON-NLS-1$
        setOutlinerContextMenuId("#ClassFileOutlinerContext"); //$NON-NLS-1$
        // don't set help contextId, we install our own help context

        decompilerFlags = new BitSet();
        // TODO take from preferences and/or last editor memento
        decompilerFlags.set(BCOConstants.F_SHOW_LINE_INFO, true);
        decompilerFlags.set(BCOConstants.F_SHOW_VARIABLES, true);
        decompilerFlags.set(BCOConstants.F_SHOW_RAW_BYTECODE, false);
        setSourceAttachmentPossible(true);
    }

    /**
     * @return the hasMappedSource
     */
    protected boolean hasMappedSource() {
        return hasMappedSource;
    }

    /**
     * @param hasMappedSource the hasMappedSource to set
     */
    protected void setHasMappedSource(boolean hasMappedSource) {
        this.hasMappedSource = hasMappedSource;
    }

    private ClassFileDocumentProvider getClassFileDocumentProvider() {
        if (fClassFileDocumentProvider == null) {
            fClassFileDocumentProvider = new BytecodeDocumentProvider(this);
        }
        return fClassFileDocumentProvider;
    }

    public void setDecompilerFlag(int flag, boolean value) {
        decompilerFlags.set(flag, value);
    }

    public boolean getDecompilerFlag(int flag) {
        return decompilerFlags.get(flag);
    }

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException {
        input = doOpenBuffer(input, false, true);
        super.init(site, input);
    }

    /**
     * Sets editor input only if buffer was actually opened.
     * @param force if <code>true</code> initialize no matter what
     * @param reuseSource true to show source code if available
     */
    public void doSetInput(boolean force, boolean reuseSource) {
        IEditorInput input = getEditorInput();
        input = doOpenBuffer(input, force, reuseSource);
        if (input != null) {
            try {
                doSetInput(input);
            } catch (Exception e) {
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            }
        }
    }

    /*
     * @see AbstractTextEditor#doSetInput(IEditorInput)
     * ClassFileDocumentProvider.setDocumentContent(IDocument, IEditorInput, String) line:
     * 202 ClassFileDocumentProvider(StorageDocumentProvider).createDocument(Object) line:
     * 228 ClassFileDocumentProvider.createDocument(Object) line: 247
     * ClassFileDocumentProvider.createElementInfo(Object) line: 275
     * ClassFileDocumentProvider(AbstractDocumentProvider).connect(Object) line: 398
     * BytecodeClassFileEditor(AbstractTextEditor).doSetInput(IEditorInput) line: 3063
     * BytecodeClassFileEditor(StatusTextEditor).doSetInput(IEditorInput) line: 173
     * BytecodeClassFileEditor(AbstractDecoratedTextEditor).doSetInput(IEditorInput) line:
     * 1511 BytecodeClassFileEditor(JavaEditor).internalDoSetInput(IEditorInput) line:
     * 2370 BytecodeClassFileEditor(JavaEditor).doSetInput(IEditorInput) line: 2343
     * BytecodeClassFileEditor.doSetInput(IEditorInput) line: 201
     * AbstractTextEditor$17.run(IProgressMonitor) line: 2396
     */
    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {

        input = transformEditorInput(input);
        if (!(input instanceof IClassFileEditorInput)) {
            throw new CoreException(JavaUIStatus.createError(
                IJavaModelStatusConstants.INVALID_RESOURCE_TYPE,
                "invalid input", // JavaEditorMessages.ClassFileEditor_error_invalid_input_message,
                null));
        }

        IDocumentProvider documentProvider = getDocumentProvider();
        if (documentProvider instanceof ClassFileDocumentProvider) {
            ((ClassFileDocumentProvider) documentProvider)
                .removeInputChangeListener(this);
        }

        super.doSetInput(input);

        documentProvider = getDocumentProvider();
        if (documentProvider instanceof ClassFileDocumentProvider) {
            ((ClassFileDocumentProvider) documentProvider)
                .addInputChangeListener(this);
        }
    }

    /**
     * @return <code>true</code> if this editor displays decompiled source,
     * <code>false</code> otherwise
     */
    public boolean isDecompiled() {
        return decompiled;
    }

    private IEditorInput doOpenBuffer(IJavaReferenceType type,
        IClassFile parent, boolean externalClass) {
        IClassFile classFile = null;
        try {
            classFile = JdtUtils.getInnerType(parent, getSourceMapper()
                .getDecompiledClass(parent), type.getSignature());
        } catch (DebugException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        return doOpenBuffer(classFile, externalClass);
    }

    private IEditorInput doOpenBuffer(IClassFile classFile, boolean externalClass) {
        IEditorInput input = null;
        if (classFile == null) {
            return null;
        }
        if (externalClass) {
            // TODO create external input, but we need a file object here...
        } else {
            input = transformEditorInput(classFile);
        }
        return doOpenBuffer(input, false, true);
    }

    private IEditorInput doOpenBuffer(IEditorInput input, boolean force,
        boolean reuseSource) {
        if (input instanceof IClassFileEditorInput) {
            IClassFile cf = ((IClassFileEditorInput) input).getClassFile();
            String origSrc = getAttachedJavaSource(cf, force);
            if (origSrc != null && !hasMappedSource) {
                // remember, that the JDT knows where the real source is and can show it
                setHasMappedSource(true);
            }

            if (origSrc == null || (force && !reuseSource)) {
                setDecompiled(true);
                char[] src;
                if (input instanceof ExternalClassFileEditorInput) {
                    ExternalClassFileEditorInput extInput = (ExternalClassFileEditorInput) input;
                    src = getSourceMapper().getSource(
                        extInput.getFile(), cf, decompilerFlags);
                } else {
                    src = getSourceMapper().getSource(cf, decompilerFlags);
                }
                changeBufferContent(cf, src);
            } else {
                setDecompiled(false);
            }
        } else if (input instanceof FileEditorInput) {
            FileEditorInput fileEditorInput = (FileEditorInput) input;
            // make class file from that
            IClassFileEditorInput cfi = (IClassFileEditorInput) transformEditorInput(input);
            // return changed reference
            input = cfi;
            setDecompiled(true);
            IClassFile cf = cfi.getClassFile();
            char[] src = getSourceMapper().getSource(
                fileEditorInput.getFile(), cf, decompilerFlags);
            changeBufferContent(cf, src);
        }
        return input;
    }

    private void setDecompiled(boolean decompiled) {
        boolean oldDecompiled = this.decompiled;
        this.decompiled = decompiled;
        if(initDone && oldDecompiled != decompiled) {
            if(decompiled) {
                // prevent multiple errors in ASTProvider which fails to work without source
                uninstallOccurrencesFinder();
            } else {
                // install again if source is available
                installOccurrencesFinder(true);
            }
        }
    }

    private static String getAttachedJavaSource(IClassFile cf, boolean force) {
        String origSrc = null;
        if (force) {
            IBuffer buffer = BytecodeBufferManager.getBuffer(cf);
            if (buffer != null) {
                BytecodeBufferManager.removeBuffer(buffer);
            }
        }
        try {
            origSrc = cf.getSource();
            if (origSrc != null && origSrc.startsWith(MARK)) {
                // this is NOT orig. sourse, but cached content
                origSrc = null;
            }
        } catch (JavaModelException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        return origSrc;
    }

    private static void changeBufferContent(IClassFile cf, char[] src) {
        IBuffer buffer = BytecodeBufferManager.getBuffer(cf);

        // i'm not sure if we need to create buffer each time -
        // couldn't we reuse existing one (if any)?
        // - seems that without "create" some listener didn't get notifications about
        // changed content
        boolean addBuffer = false;

        // if(buffer == null) {
        buffer = BytecodeBufferManager.createBuffer(cf);
        addBuffer = true;
        // }
        if (src == null) {
            src = new char[]{'\n', '/', '/', 'E', 'r', 'r', 'o', 'r'};
        }
        buffer.setContents(src);
        if (addBuffer) {
            BytecodeBufferManager.addBuffer(buffer);
            buffer.addBufferChangedListener((IBufferChangedListener) cf);
        }
    }

    @Override
    protected IJavaElement getElementAt(int offset) {

        IClassFile classFile = getClassFile();
        if (classFile == null) {
            return null;
        }
        IJavaElement result = null;
        if (isDecompiled()) {
            IDocument document = getDocumentProvider().getDocument(
                getEditorInput());
            try {
                // XXX have test if the requested line is from bytecode or sourcecode?!?
                if(document.getLength() > offset){
                    int lineAtOffset = document.getLineOfOffset(offset);
                    // get DecompiledMethod from line, then get JavaElement with same
                    // signature, because we do not have offsets or lines in the class file,
                    // only java elements...
                    result = getSourceMapper().findElement(classFile, lineAtOffset);
                }
            } catch (BadLocationException e) {
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            }
        } else {
            try {
                result = classFile.getElementAt(offset);
            } catch (JavaModelException e) {
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            }
        }

        return result;
    }

    public IClassFile getClassFile() {
        IEditorInput editorInput = getEditorInput();
        if (!(editorInput instanceof IClassFileEditorInput)) {
            return null;
        }
        return ((IClassFileEditorInput) editorInput).getClassFile();
    }

    @Override
    protected void setSelection(ISourceReference reference, boolean moveCursor) {
        if (reference == null) {
            if (moveCursor) {
                resetHighlightRange();
            }
            return;
        }
        try {
            ISourceRange range = null;
            int offset;
            int length;
            if (isDecompiled() && isSupportedMember(reference)) {

                // document lines count starts with 1 and not with 0
                int decompLine = -1
                    + getSourceMapper().getDecompiledLine(
                        (IMember) reference, getClassFile());
                if(decompLine < 0){
                    return;
                }
                IRegion region = ((BytecodeDocumentProvider) getDocumentProvider())
                    .getDecompiledLineInfo(getEditorInput(), decompLine);
                if (region == null) {
                    return;
                }
                offset = region.getOffset();
                length = region.getLength();
            } else if (!isDecompiled()) {
                range = reference.getSourceRange();
                if (range == null) {
                    return;
                }
                offset = range.getOffset();
                length = range.getLength();
            } else {
                return;
            }

            if (offset > -1 && length > 0) {
                setHighlightRange(offset, length, moveCursor);
            }

            if ((reference instanceof IMember) && !isDecompiled()) {
                IMember member = (IMember) reference;
                range = member.getNameRange();
                if (range != null) {
                    offset = range.getOffset();
                    length = range.getLength();
                }
            }
            if (moveCursor && offset > -1 && length > 0) {
                ISourceViewer sourceViewer = getSourceViewer();
                if (sourceViewer != null) {
                    sourceViewer.revealRange(offset, length);
                    sourceViewer.setSelectedRange(offset, length);
                }
            }
            return;
        } catch (Exception e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }

        if (moveCursor) {
            resetHighlightRange();
        }
    }

    private static boolean isSupportedMember(ISourceReference reference) {
        // TODO this condition is not enough. We could have inner/anon. classes
        // as selection source (they are displayed in the outline),
        // but they are not in the decompiled class,
        // so we need to filter "external" elements too, even if they are methods...

        // we could also later point to the IField's, but currently it is not supported
        return reference instanceof IMethod
            || reference instanceof IInitializer;
    }

    @Override
    public Object getAdapter(Class required) {
        if (IToggleBreakpointsTarget.class == required) {

            // TODO implement own adapter for toggle breakpoints, because the default one
            // could not find java elements in our document and therefore could not
            // create a Java breakpoint.
            // see org.eclipse.jdt.internal.debug.ui.actions.ToggleBreakpointAdapter
            // IMember member =
            // ActionDelegateHelper.getDefault().getCurrentMember(selection);
            // and the ActionDelegateHelper looks with the given offset at classfile or
            // compilation unit, but our offset is completely different to Java source
            // code
            return null;
        }

        return super.getAdapter(required);
    }

    @Override
    protected ISourceReference computeHighlightRangeSourceReference() {
        if (!isDecompiled()) {
            return super.computeHighlightRangeSourceReference();
        }

        ISourceViewer sourceViewer = getSourceViewer();
        if (sourceViewer == null) {
            return null;
        }
        StyledText styledText = sourceViewer.getTextWidget();
        if (styledText == null) {
            return null;
        }

        int caret = 0;
        if (sourceViewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) sourceViewer;
            caret = extension.widgetOffset2ModelOffset(styledText
                .getCaretOffset());
        } else {
            int offset = sourceViewer.getVisibleRegion().getOffset();
            caret = offset + styledText.getCaretOffset();
        }

        IJavaElement element = getElementAt(caret);

        if (!(element instanceof ISourceReference)) {
            return null;
        }
        return (ISourceReference) element;
    }

    @Override
    public void showHighlightRangeOnly(boolean showHighlightRangeOnly) {
        // disabled as we currently do not support "partial" view on selected
        // elements
        // super.showHighlightRangeOnly(showHighlightRangeOnly);
    }

    @Override
    protected void updateOccurrenceAnnotations(ITextSelection selection,
        CompilationUnit astRoot) {
        // disabled for bytecode as we currently do not support "occurencies" highlighting
        if(hasMappedSource()) {
            super.updateOccurrenceAnnotations(selection, astRoot);
        }
    }

    /**
     * Updater that takes care of minimizing changes of the editor input.
     */
    private class InputUpdater implements Runnable {

        /** Has the runnable already been posted? */
        private boolean fPosted = false;
        /** Editor input */
        private IClassFileEditorInput fClassFileEditorInput;

        public InputUpdater() {
            //
        }

        @Override
        public void run() {

            IClassFileEditorInput input;
            synchronized (this) {
                input = fClassFileEditorInput;
            }

            try {
                if (getSourceViewer() != null) {
                    setInput(input);
                }
            } finally {
                synchronized (this) {
                    fPosted = false;
                }
            }
        }

        /**
         * Posts this runnable into the event queue if not already there.
         * @param input the input to be set when executed
         */
        public void post(IClassFileEditorInput input) {

            synchronized (this) {
                if (fPosted) {
                    if (input != null && input.equals(fClassFileEditorInput)) {
                        fClassFileEditorInput = input;
                    }
                    return;
                }
            }

            if (input != null && input.equals(getEditorInput())) {
                ISourceViewer viewer = getSourceViewer();
                if (viewer != null) {
                    StyledText textWidget = viewer.getTextWidget();
                    if (textWidget != null && !textWidget.isDisposed()) {
                        synchronized (this) {
                            fPosted = true;
                            fClassFileEditorInput = input;
                        }
                        textWidget.getDisplay().asyncExec(this);
                    }
                }
            }
        }
    }

    @Override
    protected IJavaElement getCorrespondingElement(IJavaElement element) {
        IClassFile classFile = getClassFile();
        if (classFile == null) {
            return super.getCorrespondingElement(element);
        }
        if (classFile.equals(element.getAncestor(IJavaElement.CLASS_FILE))) {
            return element;
        }
        return super.getCorrespondingElement(element);
    }

    @Override
    public boolean isEditable() {
        return isDecompiled();
    }

    @Override
    public boolean isEditorInputReadOnly() {
        return !isDecompiled();
    }

    @Override
    public boolean isEditorInputModifiable() {
        return isDecompiled();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return isDecompiled();
    }

    /**
     * Translates the given object into an <code>IClassFileEditorInput</code>
     * @param input the object to be transformed if necessary
     * @return the transformed editor input
     */
    protected IClassFileEditorInput transformEditorInput(Object input) {

        if (input instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) input).getFile();
            Constructor<ExternalClassFileEditorInput> cons;
            try {
                cons = ExternalClassFileEditorInput.class
                    .getDeclaredConstructor(new Class[]{IFile.class});
                cons.setAccessible(true);
                IClassFileEditorInput classFileInput = cons
                    .newInstance(new Object[]{file});
                return classFileInput;
            } catch (Exception e) {
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            }
        } else if (input instanceof IClassFileEditorInput) {
            return (IClassFileEditorInput) input;
        } else if (input instanceof IClassFile) {
            return new InternalClassFileEditorInput((IClassFile) input);
        }

        return null;
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        initDone = true;
    }

    @Override
    public void inputChanged(final IClassFileEditorInput input) {
        Runnable updateInput = new Runnable() {
            @Override
            public void run() {
                fInputUpdater.post(input);
                IClassFile cf = input.getClassFile();
                try {
                    String source = cf.getSource();
                    setDecompiled(source != null && source.startsWith(MARK));
                } catch (JavaModelException e) {
                    BytecodeOutlinePlugin.log(e, IStatus.ERROR);
                }

            }
        };
        if(Display.getCurrent() == null){
            Display.getDefault().asyncExec(updateInput);
        } else {
            updateInput.run();
        }
    }

    @Override
    public void dispose() {
        // http://bugs.eclipse.org/bugs/show_bug.cgi?id=18510
        IDocumentProvider documentProvider = getDocumentProvider();
        if (documentProvider instanceof ClassFileDocumentProvider) {
            ((ClassFileDocumentProvider) documentProvider)
                .removeInputChangeListener(this);
        }

        IClassFile classFile = getClassFile();
        BytecodeBufferManager.removeBuffer(BytecodeBufferManager
            .getBuffer(classFile));
        super.dispose();
    }

    public static BytecodeSourceMapper getSourceMapper() {
        return sourceMapper;
    }

    /**
     * Check if we can show an inner class, which was declared in the source code of the
     * given parent class and which could have the bytecode for the given source line.
     *
     * If both are true, then this method changes the input of the bytecode editor
     * with the given class file (if any) to the inner class, or opens a new editor with
     * the inner class.
     *
     * @param sourceLine requested source line (from debugger)
     * @param parent expected parent class file
     * @return The region in the inner class (if inner class could be found), or an empty
     * zero-based region.
     */
    public static IRegion checkForInnerClass(int sourceLine, IClassFile parent) {
        IRegion region = new Region(0, 0);

        // get the editor with given class file, if any
        BytecodeClassFileEditor editor = getBytecodeEditor(parent);
        if (editor == null) {
            return region;
        }

        // get the inner class type according to the debugger stack frame, if any
        IJavaReferenceType debugType = sourceMapper.getLastTypeInDebugger();
        if (debugType == null) {
            return region;
        }

        boolean externalClass = editor.getEditorInput() instanceof ExternalClassFileEditorInput;
        IEditorInput input = null;
        // check if it is a inner class from the class in editor
        if (!hasInnerClass(debugType, parent)) {
            // not only inner classes could be defined in the same source file, but also
            // local types (non public non inner classes in the same source file)
            IClassFile classFile = getLocalTypeClass(debugType, parent);
            if(classFile != null){
                input = editor.doOpenBuffer(classFile, externalClass);
            }
        } else {
            // if both exists, replace the input to the inner class
            input = editor.doOpenBuffer(debugType, parent, externalClass);
        }

        if (input == null) {
            return region;
        }

        /*
         * Now we change editor input from parent class to child class.
         * It will change editor title too, so the user will see a new class.
         * After this change, we could finally compute right source line for current stack
         */
        try {
            editor.doSetInput(input);
        } catch (CoreException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            return region;
        }

        // and then map given source line to the decompiled code
        int decompiledLine = sourceMapper.mapToDecompiled(
            sourceLine + 1, editor.getClassFile());

        if (decompiledLine >= 0) {
            // and get the requested line information
            try {
                region = editor.getDocumentProvider().getDocument(input)
                    .getLineInformation(decompiledLine);
            } catch (BadLocationException e) {
                BytecodeOutlinePlugin.log(e, IStatus.ERROR);
            }
        }
        return region;
    }

    /**
     * @param debugType
     * @param parent
     * @return non public class (local type class) with the name from "debugType" and
     * which source was in the "parent" class. Null if no class file could be found.
     */
    private static IClassFile getLocalTypeClass(IJavaReferenceType debugType, IClassFile parent) {
        try {
            IType type = parent.getType();
            if((type.isLocal()) || (type.isMember())) {
                // local type could not be defined in local or inner classes
                return null;
            }
            // debugType.getSignature() == Lpackage/name/with/slashes/className;
            String binarySignature = debugType.getSignature();
            // get only type name from binary signature
            int idx = binarySignature.lastIndexOf('/');
            if(idx > 0 && idx < binarySignature.length() - 1){
                String name = binarySignature.substring(idx + 1);
                if(name.charAt(name.length() - 1) == ';'){
                    name = name.substring(0, name.length() - 1);
                }
                return type.getPackageFragment().getClassFile(name + ".class");
            }
        } catch (Exception e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        return null;
    }

    private static boolean hasInnerClass(IJavaReferenceType debugType,
        IClassFile parent) {
        try {
            String parentName = parent.getType().getFullyQualifiedName();
            String childName = debugType.getName();
            return childName != null && childName.startsWith(parentName + "$");
        } catch (Exception e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        return false;
    }

    /**
     * Returns the package fragment root of this file.
     *
     * @param file the class file
     * @return the package fragment root of the given class file
     */
    IPackageFragmentRoot getPackageFragmentRoot(IClassFile file) {

        IJavaElement element= file.getParent();
        while (element != null && element.getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT) {
            element= element.getParent();
        }

        return (IPackageFragmentRoot) element;
    }

    private static BytecodeClassFileEditor getBytecodeEditor(IClassFile parent) {
        IEditorReference[] editorReferences = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getActivePage().getEditorReferences();
        for (int i = 0; i < editorReferences.length; i++) {
            IEditorPart editor = editorReferences[i].getEditor(false);
            if (editor instanceof BytecodeClassFileEditor) {
                BytecodeClassFileEditor bytecodeEditor = (BytecodeClassFileEditor) editor;
                if (parent.equals((bytecodeEditor).getClassFile())) {
                    return bytecodeEditor;
                }
            }
        }
        return null;
    }

    public ITextSelection convertSelection(ITextSelection textSelection,
        boolean toDecompiled) {
        int startLine = textSelection.getStartLine();
        int newLine;
        if (toDecompiled) {
            newLine = sourceMapper.mapToDecompiled(
                startLine + 1, getClassFile()) + 1;
        } else {
            newLine = sourceMapper.mapToSource(startLine, getClassFile()) - 1;
        }
        if(newLine < 0) {
            return null;
        }
        IDocument document = getDocumentProvider()
            .getDocument(getEditorInput());
        try {
            int lineOffset = document.getLineOffset(newLine);
            return new TextSelection(lineOffset, 0);
        } catch (BadLocationException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        return null;
    }

    public int getSourceLine(ITextSelection bytecodeSelection) {
        int startLine = bytecodeSelection.getStartLine();
        return sourceMapper.mapToSource(startLine, getClassFile()) - 1;
    }

    public ITextSelection convertLine(int sourceLine) {
        int newLine = sourceMapper.mapToDecompiled(
            sourceLine + 1, getClassFile()) + 1;
        IDocument document = getDocumentProvider()
            .getDocument(getEditorInput());
        try {
            int lineOffset = document.getLineOffset(newLine);
            return new TextSelection(lineOffset, 0);
        } catch (BadLocationException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        return null;
    }

    public int getBytecodeInstructionAtLine(int line) {
        if (isDecompiled()) {
            DecompiledClass decompiledClass = sourceMapper.getDecompiledClass(getClassFile());
            if(line > 0 && decompiledClass != null) {
                return decompiledClass.getBytecodeInsn(line);
            }
        }
        return -1;
    }

    public void setSourceAttachmentPossible(boolean sourceAttachmentPossible) {
        this.sourceAttachmentPossible = sourceAttachmentPossible;
    }

    public boolean isSourceAttachmentPossible() {
        return sourceAttachmentPossible && isDecompiled() && !hasMappedSource();
    }
}
