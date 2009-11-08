package groovy.lang

class Pair<T1,T2> {
    T1 first
    T2 second

    Pair (T1 first, T2 second) {
        this.first = first;
        this.second = second
    }
}