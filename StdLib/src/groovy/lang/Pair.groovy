package groovy.lang

class Pair<T1,T2> {
    T1 first
    T2 second

    Pair (T1 first, T2 second) {
        this.first = first;
        this.second = second
    }

  boolean equals(o) {
    if (this.is(o)) return true;

    if (!(o instanceof Pair)) return false;

    Pair pair = (Pair) o;

    if (first != null ? !first.equals(pair.first) : pair.first != null) return false;
    if (second != null ? !second.equals(pair.second) : pair.second != null) return false;

    return true;
  }

  int hashCode() {
    int result;

    result = (first ? first.hashCode() : 0);
    result = 31 * result + (second ? second.hashCode() : 0);
    return result;
  }
}