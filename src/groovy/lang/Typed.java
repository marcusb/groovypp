package groovy.lang;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE, ElementType.PACKAGE})
@GroovyASTTransformationClass("org.mbte.groovypp.compiler.CompileASTTransform")
public @interface Typed {
    boolean debug() default false;

    TypePolicy value() default TypePolicy.STATIC;
}
