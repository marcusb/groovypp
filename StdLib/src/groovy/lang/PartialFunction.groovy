package groovy.lang

@Trait
abstract class PartialFunction<T,R> extends Function1<T,R> {
    boolean isDefined (T arg) { false }
}