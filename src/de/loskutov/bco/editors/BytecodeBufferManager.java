
package de.loskutov.bco.editors;

import java.util.Enumeration;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.BufferManager;

import de.loskutov.bco.BytecodeOutlinePlugin;


/**
 * This class is a hack that replaces JDT <code>BufferManager</code> in order
 * to make <code>addBuffer()</code> and <code>removeBuffer()</code>
 * accessible.
 * 
 * @author V.Grishchenko
 */
public class BytecodeBufferManager extends BufferManager {

    /**
     * Constructor for JadclipseBufferManager.
     */
    public BytecodeBufferManager(BufferManager manager) {
        super();
        synchronized (BufferManager.class) {
            Enumeration en = manager.getOpenBuffers();
            while (en.hasMoreElements()) {
                addBuffer((IBuffer) en.nextElement());
            }
            BufferManager.DEFAULT_BUFFER_MANAGER = this;
        }
    }

    /**
     * Closes buffers open by jadclipse
     * 
     * @param all
     *            close all buffers including those that have no real source
     */
    public static void closeJadclipseBuffers(boolean all) {
        BufferManager manager = BufferManager.getDefaultBufferManager();
        if (manager instanceof BytecodeBufferManager) {
            Enumeration en = manager.getOpenBuffers();
            while (en.hasMoreElements()) {
                IBuffer buffer = (IBuffer) en.nextElement();
                IOpenable owner = buffer.getOwner();
                if (owner instanceof IClassFile && 
                    buffer.getContents().startsWith( BytecodeClassFileEditor.MARK)) {
                    BytecodeBufferManager jManager = (BytecodeBufferManager) manager;
                    jManager.removeBuffer(buffer);
                    if (!all) { // restore buffers for files without source
                        IClassFile cf = (IClassFile) owner;
                        String realSource = null;
                        try {
                            realSource = cf.getSource();
                        } catch (JavaModelException e) {
                            IStatus err = new Status(
                                IStatus.ERROR, BytecodeClassFileEditor.ID,
                                0,
                                "failed to get source while flushing buffers",
                                e);
                            BytecodeOutlinePlugin.getDefault().getLog().log(err);
                        }
                        if (realSource == null)
                            jManager.addBuffer(buffer);
                    }
                }
            }
        }
    }

    /**
     * @see BufferManager#addBuffer(IBuffer)
     */
    public void addBuffer(IBuffer buffer) {
        super.addBuffer(buffer);
    }

    /**
     * @see BufferManager#removeBuffer(IBuffer)
     */
    public void removeBuffer(IBuffer buffer) {
        super.removeBuffer(buffer);
    }
}

