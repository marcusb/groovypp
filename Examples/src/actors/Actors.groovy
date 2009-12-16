package actors

import java.util.concurrent.atomic.AtomicReference

@Typed
public class Actors {

    static class Atom<S> extends AtomicReference<S> {
    }

}