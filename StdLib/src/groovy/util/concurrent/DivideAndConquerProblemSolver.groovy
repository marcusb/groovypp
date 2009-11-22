package groovy.util.concurrent

/**
* @author ven
*/
@Typed
class DivideAndConquerProblemSolver {

  def DivideAndConquerProblemSolver(SelfRecurringProblem problem, int nWorkers) {
    workers = (1..nWorkers).map{ new Worker(it - 1) }.asList ()

    // Prevent work stealing happening immediately, distribute the work.
    root = new Job(problem, null)
    List<Job> jobs = [root]
    List<Job> atomics = []
    while (jobs.size() > 0 && jobs.size() < nWorkers) {
      jobs = removeAtomic(jobs, atomics)
      jobs = jobs.map{ job -> job.problem.sub().map{ new Job(it, job) } }.flatten().asList()
    }
    for (int i = 0; i < jobs.size(); ++i) workers[i % nWorkers].jobs.addFirst(jobs[i])
    for (int i = 0; i < atomics.size(); ++i) workers[i % nWorkers].jobs.addFirst(atomics[i])
  }

  def solve() {
    workers.map {
      def t = new Thread(it)
      t.start()
      t
    }.each { it.join() }
    root.result
  }

  List<Job> removeAtomic(List<Job> jobs, List atomics) {
    def result = []
    for (job in jobs) {
      (job.problem.complex() ? result : atomics) << job
    }
    result
  }

  class Job {
    SelfRecurringProblem problem
    Job parent
    List<Job> children = new ArrayList<Job>()
    Object result = null

    Job(SelfRecurringProblem problem, Job parent) {
      this.problem = problem
      this.parent = parent
    }

    void setResult(Object result) {
      this.@result = result
      if (parent) {
        assert parent.children.size() > 0
        if (!parent.children.any{it.result == null}) {
          assert parent != this
          parent.setResult(problem.combine(parent.children.map{it.result}))
          parent.children = null
        }
      }
    }
  }

  class Worker implements Runnable{
    Worker(int num) { this.num = num }

    int num

    // TODO(ven): maybe can do non locking?
    class JobList extends Lockable {
      LinkedList<Job> list = new LinkedList<Job>()

      def void addFirst(Job e) {
        withLock {
          list.addFirst e
        }
      }

      Job removeFirst() {
        withLock {
          list.removeFirst()
        }
      }

      Job removeLast() {
        withLock {
          list.removeFirst()
        }
      }
    }

    JobList jobs = new JobList()

    void run() {
      Job job = null
      while (true) {
        if ((job = jobs.removeFirst()) == null) {
          // Steal work from someone else.
          int nWorkers = workers.size()
          int i = (num + 1) % nWorkers
          while (i != num) {
            if ((job = workers[i].jobs.removeLast()) != null) break
            i = (i + 1) % nWorkers
          }
        }
        if (!job) break
        if (job.problem.complex()) {
          job.problem.sub().each{
            def child = new Job(it, job)
            jobs.addFirst(child)
            job.children << child
          }
        } else {
          job.setResult(job.problem.solve());
        }
      }
    }
  }

  List<Worker> workers
  Job root
}
