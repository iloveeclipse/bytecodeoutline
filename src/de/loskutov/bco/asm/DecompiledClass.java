package de.loskutov.bco.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import de.loskutov.bco.ui.JdtUtils;

/**
 * @author Eric Bruneton
 */

public class DecompiledClass {

    public static final String ATTR_CLAS_SIZE = "class.size";
    public static final String ATTR_JAVA_VERSION = "java.version";
    public static final String ATTR_ACCESS_FLAGS = "access";

    /** key is DecompiledMethod, value is IJavaElement (Member)*/
    private Map methodToJavaElt;
    private List text;
    /**
     * key is string, value is string
     */
    private Map classAttributesMap = new HashMap();
    private String value;
    private ClassNode classNode;

    public DecompiledClass(final List text) {
        this.text = text;
        methodToJavaElt = new HashMap();
    }

    public void setAttribute(String key, String value){
        classAttributesMap.put(key, value);
    }

    /**
     * @return the class's access flags (see {@link Opcodes}). This
     *        parameter also indicates if the class is deprecated.
     */
    public int getAccessFlags(){
        int result = 0;
        String flags = (String) classAttributesMap.get(ATTR_ACCESS_FLAGS);
        if(flags == null){
            return result;
        }
        try {
            Integer intFlags = Integer.valueOf(flags);
            result = intFlags.intValue();
        } catch (NumberFormatException e) {
            // ignore, should not happen
        }
        return result;
    }

    /**
     * @return true if the class is either abstract or interface
     */
    public boolean isAbstractOrInterface(){
        int accessFlags = getAccessFlags();
        return ((accessFlags & Opcodes.ACC_ABSTRACT) != 0) ||
            ((accessFlags & Opcodes.ACC_INTERFACE) != 0);
    }

    public String getAttribute(String key){
        return (String)classAttributesMap.get(key);
    }

    public String getText() {
        if (value == null) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < text.size(); ++i) {
                Object o = text.get(i);
                if (o instanceof DecompiledMethod) {
                    buf.append(((DecompiledMethod) o).getText());
                } else {
                    buf.append(o);
                }
            }
            value = buf.toString();
        }
        return value;
    }

    public String[][] getTextTable() {
        List lines = new ArrayList();
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof DecompiledMethod) {
                String[][] mlines = ((DecompiledMethod) o).getTextTable();
                for (int j = 0; j < mlines.length; ++j) {
                    lines.add(mlines[j]);
                }
            } else {
                lines.add(new String[]{"", "", o.toString(), ""});
            }
        }
        return (String[][]) lines.toArray(new String[lines.size()][]);
    }

    public int getBytecodeOffest(final int decompiledLine) {
        int currentDecompiledLine = 0;
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof DecompiledMethod) {
                DecompiledMethod m = (DecompiledMethod) o;
                Integer offset = m.getBytecodeOffset(decompiledLine - currentDecompiledLine);
                if(offset != null){
                    return offset.intValue();
                }
                currentDecompiledLine += m.getLineCount();
            } else {
                currentDecompiledLine++;
            }
        }
        return -1;
    }

    public int getBytecodeInsn(final int decompiledLine) {
      int currentDecompiledLine = 0;
      for (int i = 0; i < text.size(); ++i) {
          Object o = text.get(i);
          if (o instanceof DecompiledMethod) {
              DecompiledMethod m = (DecompiledMethod) o;
              Integer opcode = m.getBytecodeInsn(decompiledLine - currentDecompiledLine);
              if(opcode != null){
                  return opcode.intValue();
              }
              currentDecompiledLine += m.getLineCount();
          } else {
              currentDecompiledLine++;
          }
      }
      return -1;
  }

    public int getSourceLine(final int decompiledLine) {
        int currentDecompiledLine = 0;
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof DecompiledMethod) {
                DecompiledMethod m = (DecompiledMethod) o;
                int l = m.getSourceLine(decompiledLine - currentDecompiledLine);
                if (l != -1) {
                    return l;
                }
                currentDecompiledLine += m.getLineCount();
            } else {
                currentDecompiledLine++;
            }
        }
        return -1;
    }

    public DecompiledMethod getMethod(final int decompiledLine) {
        int currentDecompiledLine = 0;
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof DecompiledMethod) {
                DecompiledMethod m = (DecompiledMethod) o;
                int l = m.getSourceLine(decompiledLine - currentDecompiledLine);
                if (l != -1) {
                    return m;
                }
                currentDecompiledLine += m.getLineCount();
            } else {
                currentDecompiledLine++;
            }
        }
        return null;
    }

    public DecompiledMethod getMethod(final String signature) {
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof DecompiledMethod) {
                DecompiledMethod m = (DecompiledMethod) o;
                if(signature.equals(m.getSignature())){
                    return m;
                }
            }
        }
        return null;
    }

    public IJavaElement getJavaElement(int decompiledLine, IClassFile clazz){
        DecompiledMethod method = getMethod(decompiledLine);
        if(method != null){
            IJavaElement javaElement = (IJavaElement) methodToJavaElt.get(method);
            if(javaElement == null) {
                javaElement = JdtUtils.getMethod(clazz, method.getSignature());
                if(javaElement != null) {
                    methodToJavaElt.put(method, javaElement);
                } else {
                    javaElement = clazz;
                }
            }
            return javaElement;
        }
        return clazz;
    }

    public int getDecompiledLine(String methSignature){
        int currentDecompiledLine = 0;
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof DecompiledMethod) {
                DecompiledMethod m = (DecompiledMethod) o;
                if(methSignature.equals(m.getSignature())){
                    return currentDecompiledLine;
                }
                currentDecompiledLine += m.getLineCount();
            } else {
                currentDecompiledLine++;
            }
        }
        return 0;
    }

    /**
     *
     * @param decompiledLine
     * @return array with two elements, first is the local variables table,
     * second is the operands stack content. "null" value could be returned too.
     */
    public String [] getFrame(final int decompiledLine, final boolean showQualifiedNames) {
      int currentDecompiledLine = 0;
      for (int i = 0; i < text.size(); ++i) {
          Object o = text.get(i);
          if (o instanceof DecompiledMethod) {
              DecompiledMethod m = (DecompiledMethod) o;
              String [] frame = m.getFrame(decompiledLine
                  - currentDecompiledLine, showQualifiedNames);
              if (frame != null) {
                  return frame;
              }
              currentDecompiledLine += m.getLineCount();
          } else {
              currentDecompiledLine++;
          }
      }
      return null;
    }

    public String[][][] getFrameTables(final int decompiledLine, boolean useQualifiedNames) {
      int currentDecompiledLine = 0;
      for (int i = 0; i < text.size(); ++i) {
          Object o = text.get(i);
          if (o instanceof DecompiledMethod) {
              DecompiledMethod m = (DecompiledMethod) o;
              String[][][] frame = m.getFrameTables(decompiledLine - currentDecompiledLine, useQualifiedNames);
              if (frame != null) {
                   return frame;
              }
              currentDecompiledLine += m.getLineCount();
          } else {
              currentDecompiledLine++;
          }
      }
      return null;
    }

    public int getDecompiledLine(final int sourceLine) {
        int currentDecompiledLine = 0;
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof DecompiledMethod) {
                DecompiledMethod m = (DecompiledMethod) o;
                int l = m.getDecompiledLine(sourceLine);
                if (l != -1) {
                    return l + currentDecompiledLine;
                }
                currentDecompiledLine += m.getLineCount();
            } else {
                currentDecompiledLine++;
            }
        }
        return -1;
    }

    public List getErrorLines() {
        List errors = new ArrayList();
        int currentDecompiledLine = 0;
        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof DecompiledMethod) {
                DecompiledMethod m = (DecompiledMethod) o;
                int l = m.getErrorLine();
                if (l != -1) {
                    errors.add(new Integer(l + currentDecompiledLine));
                }
                currentDecompiledLine += m.getLineCount();
            } else {
                currentDecompiledLine++;
            }
        }
        return errors;
    }

    public int getBestDecompiledMatch(int sourceLine) {
        int bestMatch = -1;

        for (int i = 0; i < text.size(); ++i) {
            Object o = text.get(i);
            if (o instanceof DecompiledMethod) {
                DecompiledMethod m = (DecompiledMethod) o;
                int line = m.getFirstSourceLine();
                if(line >= sourceLine){
                    if(bestMatch == -1 || line < bestMatch){
                        bestMatch = line;
                    }
                }
            }
        }
        return bestMatch;
    }

    public void setClassNode(ClassNode classNode) {
        this.classNode = classNode;
    }

    public ClassNode getClassNode(){
        return classNode;
    }
}
