package groovy.util.concurrent

@Typed
interface SelfRecurringProblem {
  boolean complex()
  List<SelfRecurringProblem> sub()
  Object solve()
  Object combine(Collection subResults)
}
