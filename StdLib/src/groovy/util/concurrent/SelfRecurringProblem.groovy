package groovy.util.concurrent

/**
 * @author ven
 */
@Typed
interface SelfRecurringProblem {
  boolean complex()
  List<SelfRecurringProblem> sub()
  Object solve()
  Object combine(Collection subResults)
}
