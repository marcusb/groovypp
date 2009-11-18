package groovy.util.concurrent

import java.util.concurrent.LinkedBlockingDeque

/**
 * @author ven
 */

@Typed
class DivideAndConquerProblemSolver {

  def DivideAndConquerProblemSolver(SelfRecurringProblem problem, int nWorkers) {
    workers = (1..nWorkers).map{ new Worker(it - 1) }.asList ()

    // Prevent work stealing happening immediately, distribute the work.
    def jobs = [new Job(problem, null)]
    def atomics = []
    while (jobs.size() > 0 && jobs.size() < nWorkers) {
      jobs = removeAtomic(jobs, atomics)
      jobs = jobs.flatMap{ job -> job.problem.sub().map{ new Job(it, job) } }
    }
    for (int i = 0; i < jobs.size(); ++i) workers[i % nWorkers].jobs << jobs[i]
    for (int i = 0; i < atomics.size(); ++i) workers[i % nWorkers].jobs << atomics[i]

    workers.map {
      def t = new Thread(it)
      t.start()
      t
    }.each { it.join() }
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
    List<Job> children
    Object result = null

    def Job(SelfRecurringProblem problem, Job parent) {
      this.problem = problem
      this.parent = parent
    }

    def setResult(Object result) {
      this.result = result
      if (parent) {
        assert parent.children
        if (!parent.children.any{it.result == null}) {
          parent.setResult(problem.combine(parent.children.map{it.result}))
          parent.children = null
        }
      }
    }
  }

  class Worker implements Runnable{
    def Worker(int num) { this.num = num }

    int num

    def jobs = new LinkedBlockingDeque<Job>()

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
          job.problem.sub().each{ jobs.addFirst(new Job(it, job)) }
        } else {
          job.setResult(job.problem.solve());
        }
      }
    }
  }

  List<Worker> workers
}
