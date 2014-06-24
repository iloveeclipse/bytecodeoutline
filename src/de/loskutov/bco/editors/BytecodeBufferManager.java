package de.loskutov.bco.editors;

import java.lang.reflect.Method;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.internal.core.BufferManager;

/**
 * This class is a hack that mades JDT <code>BufferManager</code> methods
 * <code>addBuffer()</code> and <code>removeBuffer()</code> accessible.
 *
 * It does NOT modify or replace the instance of default buffer manager, because if they
 * are more then one plugin which performs such hack, then it will end up in a big bang.
 *
 * @author Andrei
 */
public final class BytecodeBufferManager {

    private BytecodeBufferManager() {
        super();
    }

    public static IBuffer getBuffer(IOpenable owner) {
        return BufferManager.getDefaultBufferManager().getBuffer(owner);
    }

    public static IBuffer createBuffer(IOpenable owner) {
        BufferManager.getDefaultBufferManager();
        return BufferManager.createBuffer(owner);
    }

    /**
     * @see BufferManager#addBuffer(IBuffer)
     */
    public static void addBuffer(IBuffer buffer) {
        BufferManager manager = BufferManager.getDefaultBufferManager();
        try {
            Method addMethod = BufferManager.class.getDeclaredMethod(
                "addBuffer", new Class[]{IBuffer.class});
            addMethod.setAccessible(true);
            addMethod.invoke(manager, new Object[]{buffer});
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @see BufferManager#removeBuffer(IBuffer)
     */
    public static void removeBuffer(IBuffer buffer) {
        if (buffer != null) {
            BufferManager manager = BufferManager.getDefaultBufferManager();
            try {
                Method removeMethod = BufferManager.class.getDeclaredMethod(
                    "removeBuffer", new Class[]{IBuffer.class});
                removeMethod.setAccessible(true);
                removeMethod.invoke(manager, new Object[]{buffer});
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
