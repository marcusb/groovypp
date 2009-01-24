package groovy.lang;

public interface OwnerAware {
    public Object getOwner ();

    public interface Setter extends OwnerAware {
        public void setOwner (Object owner);
    }
}
