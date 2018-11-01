package de.loskutov.bco.editors;

import java.util.BitSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.BufferManager;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;

import de.loskutov.bco.BytecodeOutlinePlugin;
import de.loskutov.bco.asm.DecompiledClass;
import de.loskutov.bco.asm.DecompilerHelper;
import de.loskutov.bco.asm.DecompilerOptions;

/**
 * Overriden to get control over document content for bytecode editors
 * @author Andrei
 */
public class BytecodeDocumentProvider extends ClassFileDocumentProvider {

    private final BitSet decompilerFlags;
    private DecompiledClass decompiledClass;

    public BytecodeDocumentProvider(BitSet decompilerFlags) {
        super();
        this.decompilerFlags = decompilerFlags;
    }

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
                if(source == null) {
                    StringBuilder sb = new StringBuilder("// DECOMPILED FROM: ");
                    sb.append(classFile.getPath()).append("\n");
                    decompile(classFile, sb);
                    source = sb.toString();
                }
            }
            document.set(source);
            return true;
        }
        return super.setDocumentContent(document, editorInput, encoding);
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

    protected void decompile(IClassFile cf, StringBuilder source) {
        try {
            decompiledClass = DecompilerHelper
                .getDecompiledClass(
                    cf.getBytes(),
                    new DecompilerOptions(null, null, decompilerFlags, null));
            source.append(decompiledClass.getText());
        } catch (JavaModelException e) {
            decompiledClass = null;
            source.append(e.toString());
            BytecodeOutlinePlugin.log(e, IStatus.ERROR);
        }
    }

    public DecompiledClass getDecompiledClass() {
        return decompiledClass;
    }
}
