package groovy.lang

@Trait
abstract class Function0<R> {
    abstract def apply ()

    public <R1> Function0<R1> addThen (Function1<R,R1> g) {
        { -> g.apply(apply()) }
    }
}