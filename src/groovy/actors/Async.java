package groovy.actors;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Methods annotated by @Async will lead to creating another one, which can be called asynchroniously
 * New method will always return void and have same parameters plus one additional parameter of type 
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@GroovyASTTransformationClass("org.mbte.groovypp.compiler.AsyncASTTransform")
public @interface Async {
    public abstract String  messageClassName  () default "";
}