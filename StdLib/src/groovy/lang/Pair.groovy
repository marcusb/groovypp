package groovy.lang

@Typed
class Pair<T1, T2> {
  T1 first
  T2 second

  Pair(T1 first, T2 second) {
    this.first = first;
    this.second = second
  }

  boolean equals(obj) {
    this.is(obj) || (obj instanceof Pair && eq(first, ((Pair) obj).first) && eq(second, ((Pair) obj).second))
  }

  private boolean eq(obj1, obj2) {
    obj1 == null ? obj2 == null : obj1.equals(obj2)
  }

  int hashCode() {
    31 * hash(first) + hash(second)
  }

  private int hash(obj) {
    return obj != null ? obj.hashCode() : 0
  }
}