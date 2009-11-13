package groovy.lang

@Trait
abstract class PartialFunction<T,R> implements Function1<T,R> {
    boolean isDefined (T arg) { false }
}