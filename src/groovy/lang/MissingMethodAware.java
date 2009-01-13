package groovy.lang;

/**
 * Marker interface used by static compiler in case if it is not able to find method/property to execute.
 *
 * Compiler will generate calls to normal methods of GroovyObject when not able to find method/property to use.
 */
public interface MissingMethodAware extends GroovyObject {
}
