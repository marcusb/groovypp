package org.mbte.groovypp.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HasDefaultImplementation {
    /**
     * @return class defining static method with additional 1st parameter self
     */
    public abstract Class value();

    /**
     */
    public abstract String fieldName() default "";
}