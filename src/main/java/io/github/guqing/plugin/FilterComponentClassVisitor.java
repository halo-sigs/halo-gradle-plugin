package io.github.guqing.plugin;

import java.util.Set;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.AnnotationNode;

public class FilterComponentClassVisitor extends ClassVisitor {

    private boolean isComponentClass;

    public static final Set<String> COMPONENT_ANNOTATIONS =
        Set.of("Lorg/springframework/stereotype/Component;",
            "Lorg/springframework/stereotype/Controller;",
            "Lorg/springframework/stereotype/Service;",
            "Lorg/springframework/stereotype/Repository;",
            "Lorg/springframework/context/annotation/Configuration;",
            "Lorg/pf4j/Extension;");

    public FilterComponentClassVisitor(int api) {
        super(api);
    }

    public FilterComponentClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationNode annotationNode = new AnnotationNode(descriptor);
        if (visible && COMPONENT_ANNOTATIONS.contains(descriptor)) {
            this.isComponentClass = true;
        }
        return annotationNode;
    }

    public boolean isComponentClass() {
        return this.isComponentClass;
    }
}
