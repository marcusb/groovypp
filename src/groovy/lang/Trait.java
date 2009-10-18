package groovy.lang;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes annotated with Trait annotation will be converted to interfaces
 * All methods defined in such classes will be used as default implementation for classes implementing the interface
 * <p/>
 * The real implementation will be converted to static methods in static inner class TraitImpl
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass("org.mbte.groovypp.compiler.TraitASTTransform")
public @interface Trait {
}