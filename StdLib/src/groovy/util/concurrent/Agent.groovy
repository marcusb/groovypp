package groovy.util.concurrent

@Typed class Agent<T> extends FairExecutingChannel<Function1<T,T>> {
    private volatile T ref

    Agent () {}

    Agent (T ref) { this.@ref = ref }
    
    final T get () { ref }

    void call (Function1<T,T> mutation) {
        post mutation
    }

    void onMessage(Function1<T, T> message) {
        ref = message(ref)
    }
}
