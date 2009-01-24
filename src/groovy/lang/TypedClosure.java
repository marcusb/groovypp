package groovy.lang;

import org.codehaus.groovy.runtime.GeneratedClosure;

import java.io.Serializable;

/**
 * Closure with delegate of defined type
 *
 * @param <T> type of delegate
 */
public interface TypedClosure<T> extends GroovyObject, Cloneable, Runnable, Serializable, OwnerAware {

    /**
     * Sets the strategy which the closure uses to resolve property references. The default is Closure.OWNER_FIRST
     *
     * @param resolveStrategy The resolve strategy to set
     *
     * @see groovy.lang.Closure#DELEGATE_FIRST
     * @see groovy.lang.Closure#DELEGATE_ONLY
     * @see groovy.lang.Closure#OWNER_FIRST
     * @see groovy.lang.Closure#OWNER_ONLY
     * @see groovy.lang.Closure#TO_SELF
     */
    public void setResolveStrategy(int resolveStrategy);

    /**
     * Gets the strategy which the closure users to resolve methods and properties
     *
     * @return The resolve strategy
     *
     * @see groovy.lang.Closure#DELEGATE_FIRST
     * @see groovy.lang.Closure#DELEGATE_ONLY
     * @see groovy.lang.Closure#OWNER_FIRST
     * @see groovy.lang.Closure#OWNER_ONLY
     * @see groovy.lang.Closure#TO_SELF
     */
    public int getResolveStrategy();

    public Object getThisObject();

    /**
     * Invokes the closure without any parameters, returning any value if applicable.
     *
     * @return the value if applicable or null if there is no return statement in the closure
     */
    public Object call();

    public Object call(Object[] args);

    /**
     * Invokes the closure, returning any value if applicable.
     *
     * @param arguments could be a single value or a List of values
     * @return the value if applicable or null if there is no return statement in the closure
     */
    public Object call(final Object arguments);


    /**
     * @return the owner Object to which method calls will go which is
     *         typically the outer class when the closure is constructed
     */
    public Object getOwner();

    /**
     * @return the delegate Object to which method calls will go which is
     *         typically the outer class when the closure is constructed
     */
    public T getDelegate();

    /**
     * Allows the delegate to be changed such as when performing markup building
     *
     * @param delegate the new delegate
     */
    public void setDelegate(T delegate);

    /**
     * @return the parameter types of the longest doCall method
     * of this closure
     */
    public Class[] getParameterTypes();

    /**
     * @return the maximum number of parameters a doCall methos
     * of this closure can take
     */
    public int getMaximumNumberOfParameters();


    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run();

    /**
     * Support for closure currying
     *
     * @param arguments the arguments to bind
     * @return the new closure with its arguments bound
     */
    public Closure curry(final Object arguments[]);

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    public Object clone();


    /**
     * @return Returns the directive.
     */
    public int getDirective();

    /**
     * @param directive The directive to set.
     */
    public void setDirective(int directive);
}
