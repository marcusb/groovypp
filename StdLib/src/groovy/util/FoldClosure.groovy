package groovy.util

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport

@Typed
abstract class FoldClosure<T1,T2,R> extends DefaultGroovyMethodsSupport {
  abstract R transform(T1 t1, T2 t2);
  
  public static <T,R> R foldLeft(Collection<T> self, R init, FoldClosure<T,R,R> transform) {
    R res = init
    for (T t : self) {
      res = transform.transform(t, res)
    }
    res
  }

  public static <T,R> R foldLeft(T[] self, R init, FoldClosure<T,R,R> transform) {
    R res = init
    for (T t : self) {
      res = transform.transform(t, res)
    }
    res
  }

  public static <T,R> R foldRight(T[] self, R init, FoldClosure<T,R,R> transform) {
    R res = init
    for (int i = self.length - 1; i >= 0; --i) {
      res = transform.transform(self[i], res)
    }
    res
  }

  public static <T,R> R foldRight(ArrayList<T> self, R init, FoldClosure<T,R,R> transform) {
    R res = init
    for (int i = self.size() - 1; i >= 0; --i) {
      res = transform.transform(self.get(i), res)
    }
    res
  }
}
