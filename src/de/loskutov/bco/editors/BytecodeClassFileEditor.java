/* $Id: BytecodeClassFileEditor.java,v 1.1 2005-01-09 10:08:28 ekuleshov Exp $ */

package de.loskutov.bco.editors;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.BufferManager;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import de.loskutov.bco.BytecodeOutlinePlugin;

/**
 * A "better" way to hook into JDT...
 * 
 * @author Eugene Kuleshov, V. Grishchenko, Jochen Klein
 */
public class BytecodeClassFileEditor extends ClassFileEditor {
    public static final String ID = "de.loskutov.bco.editors.BytecodeClassFileEditor";
    public static final String MARK = "// class";
    // private static final char[] MARK_ARRAY = MARK.toCharArray();

    private BytecodeSourceMapper sourceMapper;

    /**
     * Constructor for JadclipseClassFileEditor.
     */
    public BytecodeClassFileEditor() {
        super();
    }

    protected BytecodeBufferManager getBufferManager() {
        BufferManager defManager = BufferManager.getDefaultBufferManager();
        if (defManager instanceof BytecodeBufferManager) {
            return (BytecodeBufferManager) defManager;
        }
        return new BytecodeBufferManager(defManager);
    }

    /*
     * @see IEditorPart#init(IEditorSite, IEditorInput)
     */
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException {
        doOpenBuffer(input, false);
        super.init(site, input);
    }

    /**
     * Sets edditor input only if buffer was actually opened.
     * 
     * @param force
     *            if <code>true</code> initialize no matter what
     */
    public void doSetInput(boolean force) {
        IEditorInput input = getEditorInput();
        if (doOpenBuffer(input, force)) {
            try {
                doSetInput(input);
            } catch (Exception e) {
                BytecodeOutlinePlugin.logError(e);
            }
        }
    }

    /**
     * @return <code>true</code> if this editor displays decompiled source,
     *         <code>false</code> otherwise
     */
    public boolean containsDecompiled() {
        return (sourceMapper != null);
    }

    private boolean doOpenBuffer(IEditorInput input, boolean force) {
        if (input instanceof IClassFileEditorInput) {
            try {
                boolean opened = false;
                IClassFile cf = ((IClassFileEditorInput) input).getClassFile();
                // IPreferenceStore prefs = JadclipsePlugin.getDefault().getPreferenceStore();
                // boolean reuseBuf = prefs.getBoolean(JadclipsePlugin.REUSE_BUFFER);
                // boolean always = prefs.getBoolean(JadclipsePlugin.IGNORE_EXISTING);
                boolean reuseBuf = false;
                boolean always = false;
                
                String origSrc = cf.getSource();
                
                // have to check our mark since all line comments are stripped
                // in debug align mode
                if (origSrc == null 
                    || always && !origSrc.startsWith(MARK)
                    || (origSrc.startsWith(MARK) && (!reuseBuf || force))) {
                    if (sourceMapper == null)
                        sourceMapper = new BytecodeSourceMapper();
                    char[] src = sourceMapper.findSource(cf.getType());
                    
                    if (src == null) {
                        src = new char[]{'\n', '/', '/', 'E', 'r', 'r', 'o', 'r', '!'};
                    }
                    
                    // char[] markedSrc = new char[MARK_ARRAY.length + src.length];
                    // // next time we know this is decompiled source
                    // System.arraycopy(MARK_ARRAY, 0, markedSrc, 0, MARK_ARRAY.length);
                    // System.arraycopy(src, 0, markedSrc, MARK_ARRAY.length, src.length);
                    
                    IBuffer buffer = getBufferManager().createBuffer(cf);
                    buffer.setContents(src);
                    getBufferManager().addBuffer(buffer);
                    // buffer.addBufferChangedListener((IBufferChangedListener)cf);
                    sourceMapper.mapSource(cf.getType(), src, true);
                    opened = true;
                }
                return opened;
            } catch (Exception e) {
                BytecodeOutlinePlugin.logError(e);
            }
        }
        return false;
    }

    /*
     * @see JavaEditor#getElementAt(int)
     */
    protected IJavaElement getElementAt(int offset) {
        IJavaElement result = super.getElementAt(offset);

        if (result == null && getEditorInput() instanceof IClassFileEditorInput
            && containsDecompiled()) {
            try {
                IClassFileEditorInput input = (IClassFileEditorInput) getEditorInput();
                result = sourceMapper.findElement(input.getClassFile().getType(), offset);
            } catch (JavaModelException x) {
                BytecodeOutlinePlugin.logError(x);
            }
        }
        return result;
    }

    // protected IClassFile findClassFileParent(IJavaElement jElement)
    // {
    // IJavaElement parent = jElement.getParent();
    // if (parent == null)
    // return null;
    // else if (parent instanceof IClassFile)
    // return (IClassFile)parent;
    // else
    // return findClassFileParent(jElement);
    // }

    protected void setSelection(ISourceReference reference, boolean moveCursor) {
        if (reference != null) {
            try {
                ISourceRange range = null;

                if ((reference instanceof IJavaElement) && containsDecompiled())
                    range = sourceMapper.getSourceRange((IJavaElement) reference);
                else
                    range = reference.getSourceRange();

                int offset = range.getOffset();
                int length = range.getLength();

                if (offset > -1 && length > 0)
                    setHighlightRange(offset, length, moveCursor);

                if (moveCursor && (reference instanceof IMember)) {
                    IMember member = (IMember) reference;
                    range = containsDecompiled()
                        ? sourceMapper.getNameRange(member)
                        : member.getNameRange();

                    offset = range.getOffset();
                    length = range.getLength();

                    if (range != null && offset > -1 && length > 0) {
                        if (getSourceViewer() != null) {
                            getSourceViewer().revealRange(offset, length);
                            getSourceViewer().setSelectedRange(offset, length);
                        }
                    }
                }
                return;
            } catch (Exception e) {
                BytecodeOutlinePlugin.error("", e);
            }
        }

        if (moveCursor)
            resetHighlightRange();
    }
}

