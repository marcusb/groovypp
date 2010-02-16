package groovy.lang;

import org.mbte.groovypp.runtime.HasDefaultImplementation;

/**
 * Marker interface for types which has not only own methods
 * but also able to delegate to other ones
 *
 * @param <D> type of delegate
 */
public interface Delegating<D> extends Cloneable {
    D getDelegate ();
    void setDelegate (D newDelegate);

    @HasDefaultImplementation(Delegating.TraitImpl.class)
    public Delegating<D> clone(D newDelegate) throws CloneNotSupportedException;

    public Object clone() throws CloneNotSupportedException;

    public static class TraitImpl {
        public static <D> Object clone(Delegating<D> self, D delegate) throws CloneNotSupportedException {
            Delegating<D> cloned = (Delegating<D>) self.clone();
            cloned.setDelegate(delegate);
            return cloned;
        }
    }
}
