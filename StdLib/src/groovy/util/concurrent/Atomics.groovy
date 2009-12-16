package groovy.util.concurrent

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

public class Atomics {
    static <S> void apply (AtomicReference<S> state, Function1<S,S> mutation) {
        for (;;) {
            def s = state.get()
            if (state.compareAndSet(s, mutation(s)))
                break
        }
    }

    static void apply (AtomicInteger state, Function1<Integer,Integer> mutation) {
        for (;;) {
            def s = state.get()
            if (state.compareAndSet(s, mutation(s)))
                break
        }
    }

    static void apply (AtomicLong state, Function1<Long,Long> mutation) {
        for (;;) {
            def s = state.get()
            if (state.compareAndSet(s, mutation(s)))
                break
        }
    }

    static void apply (AtomicBoolean state, Function1<Boolean,Boolean> mutation) {
        for (;;) {
            def s = state.get()
            if (state.compareAndSet(s, mutation(s)))
                break
        }
    }

    static <S> boolean tryApply (AtomicReference<S> state, Function1<S,S> mutation) {
        def s = state.get()
        state.compareAndSet(s, mutation(s))
    }

    static boolean tryApply (AtomicInteger state, Function1<Integer,Integer> mutation) {
        def s = state.get()
        state.compareAndSet(s, mutation(s))
    }

    static boolean tryApply (AtomicLong state, Function1<Long,Long> mutation) {
        def s = state.get()
        state.compareAndSet(s, mutation(s))
    }

    static boolean tryApply (AtomicBoolean state, Function1<Boolean,Boolean> mutation) {
        def s = state.get()
        state.compareAndSet(s, mutation(s))
    }
}