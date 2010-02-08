package groovy.lang;

/**
 * Marker interface for types which has not only own methods
 * but also able to delegate to other ones
 *
 * @param <D> type of delegate
 */
public interface Delegating<D> {
    D getDelegate ();
    void setDelegate (D newDelegate);
}
