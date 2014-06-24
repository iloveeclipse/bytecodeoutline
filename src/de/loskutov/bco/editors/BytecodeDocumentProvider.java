package de.loskutov.bco.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.BufferManager;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import de.loskutov.bco.BytecodeOutlinePlugin;

/**
 * Overriden to get control over document content for bytecode editors
 * @author Andrei
 */
public class BytecodeDocumentProvider extends ClassFileDocumentProvider {

    public BytecodeDocumentProvider(BytecodeClassFileEditor classFileEditor) {
        super();
    }


    /*
     * Overriden to get control over document content for bytecode editors
     * @see StorageDocumentProvider#setDocumentContent(IDocument, IEditorInput)
     */
    @Override
    protected boolean setDocumentContent(IDocument document,
        IEditorInput editorInput, String encoding) throws CoreException {

        if (editorInput instanceof IClassFileEditorInput) {
            IClassFile classFile = ((IClassFileEditorInput) editorInput)
                .getClassFile();

            String source = null;
            try {
                source = classFile.getSource();
            } catch (JavaModelException e) {
                // ignore, this may happen if *class* file is not on class path but inside
                // of source tree without associated source
            }
            if (source == null) {
                // this could be the case for class files which are not on the class path
                // buffer should be already opened and created in our editor->doOpenBuffer
                // method
                IBuffer buffer = BufferManager.getDefaultBufferManager().getBuffer(classFile);
                if (buffer != null) {
                    source = buffer.getContents();
                }
            }
            document.set(source);
            return true;
        }
        return super.setDocumentContent(document, editorInput, encoding);
    }

    /**
     *
     * During DEBUG session, debugger tries to get line information for the current line
     * in the stack, and then uses this info to set cursor and select text in editor.
     *
     * The problem is, that Java debugger knows only "source" - based lines, but our editor
     * contains "bytecode" text, where the lines are NOT aligned to the source code lines.
     * (it is simply not possible).
     *
     * So if we do not change the default implementation, the selected bytecode text will
     * never match requested sourcecode lines.
     *
     * As workaround we "just" pass to the debugger another document, as one we use to
     * represent the text in our editor. This document is a proxy and just replaces
     * the implementation of "getLineInformation" method. Our implementation mapps the
     * requested sourcecode line to the bytecode line in editor.
     *
     * All other clients of this method shouldn't be affected and should receive always
     * the original document.
     */
    @Override
    public IDocument getDocument(Object element) {
        IDocument document = super.getDocument(element);
        if (element instanceof IClassFileEditorInput && isDebuggerCall()) {
            IClassFileEditorInput input = (IClassFileEditorInput) element;

            return new DocumentProxy4Debugger(document, input.getClassFile());
        }
        return document;
    }

    /**
     * We are looking for two stack patterns, which both are related to DEBUG session and
     * coming from SourceLookupFacility.display(ISourceLookupResult result, IWorkbenchPage page):
     * first is the highlighting the editor current line, corresponding to
     * the line in the bytecode stack (light gray color),
     * and second is the annotation the current debugger position (same line as before) in
     * editor (light green color)
     *
     * This is a VERY BAD and VERY DIRTY hack, but it works.
     * @return
     */
    private static boolean isDebuggerCall() {
        Exception e = new Exception();
        StackTraceElement[] stackTrace = e.getStackTrace();
        boolean stackOk = true;
        // at 0 is our method name, and 1 id the "getDocument" call, so we start with 2
        for (int i = 2; i < stackTrace.length; i++) {
            StackTraceElement elt = stackTrace[i];
            switch (i) {
                case 2 :
                    stackOk = "getLineInformation".equals(elt.getMethodName())
                        || "addAnnotation".equals(elt.getMethodName());
                    break;
                case 3 :
                    stackOk = "positionEditor".equals(elt.getMethodName())
                     || "display".equals(elt.getMethodName());
                    break;
                default :
                    break;
            }
            if(! stackOk || i > 3){
                return false;
            }

            if (stackOk && i == 3) {
                IEditorPart activeEditor = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage()
                    .getActiveEditor();
                if(activeEditor instanceof BytecodeClassFileEditor){
                    BytecodeClassFileEditor editor = (BytecodeClassFileEditor) activeEditor;
                    return editor.isDecompiled();
                }
            }
        }
        return false;
    }

    public IRegion getDecompiledLineInfo(IEditorInput input, int decompiledLine) {
        IDocument document = getDocument(input);
        try {
            return document.getLineInformation(decompiledLine);
        } catch (BadLocationException e) {
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
        return null;
    }

    /*
     * SourceLookupFacility.class: The code is responsible for the
     * positioning of current debugger line during debugging Positions the text editor for
     * the given stack frame :
     * private void positionEditor(ITextEditor editor, IStackFrame frame)
     * private IRegion getLineInformation(ITextEditor editor, int lineNumber)
     *
     * Other place for DEBUG instruction pointer with the "wrong line" is InstructionPointerManager
     *  public void addAnnotation(ITextEditor textEditor, IStackFrame frame, Annotation annotation)
     */

    /**
     * This class is non-functional replacement for IDocument. The only one purpose is to
     * override getLineInformation() implementation for DEBUG purposes
     */
    private static final class DocumentProxy4Debugger extends AbstractDocument {

        private final IDocument delegate;
        private final IClassFile cf;

        public DocumentProxy4Debugger(IDocument delegate, IClassFile cf) {
            super();
            this.delegate = delegate;
            this.cf = cf;
        }

        @Override
        public IRegion getLineInformation(int line) throws BadLocationException {
            BytecodeSourceMapper mapper = BytecodeClassFileEditor
                .getSourceMapper();

            int decompiledLine;
            if(line < -1){
                /* this is the case if debugger does not have line information in bytecode
                 * if bytecode does not contain line info, we should at least
                 * return the line with the first method instruction.
                 */
                decompiledLine = mapper.mapDebuggerToDecompiled(cf);
            } else {
                // SourceLookupFacility decrement source line by 1
                decompiledLine = mapper.mapToDecompiled(line + 1, cf);
                if(decompiledLine == -1){
                    /*
                     * The line is from inner class (it is in another class file)
                     * the mapping does not work for inner/anon. classes, as the debugger
                     * expect that their source code is in our editor which is not the case for
                     * bytecode of inner classes => for inner classes we use another strategy
                     */
                    return BytecodeClassFileEditor.checkForInnerClass(line, cf);
                }
            }
            // editor start lines with 1
            return delegate.getLineInformation(decompiledLine + 1);
        }
    }

}
