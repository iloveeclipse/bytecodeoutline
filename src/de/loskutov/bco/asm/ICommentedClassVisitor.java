package de.loskutov.bco.asm;

import java.util.List;

import org.objectweb.asm.ClassVisitor;


public interface ICommentedClassVisitor {
    ClassVisitor getClassVisitor();
    List getText();
}
