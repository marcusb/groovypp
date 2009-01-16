package groovy.lang;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Methods annotated by @IDef will lead to creating one-method interfaces with signature as of annotated method
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@GroovyASTTransformationClass("org.mbte.groovypp.compiler.IDefASTTransform")
public @interface IDef {
    /**
     * Name of interface
     */
    String  interfaceName () default "";

    /**
     * Name of property
     */
    String  propertyName  () default "";
}
