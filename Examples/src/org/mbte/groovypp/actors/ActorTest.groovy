@Typed
package org.mbte.groovypp.actors

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

public class ActorTest extends GroovyTestCase {
  void testActor() {
    def printer = Actor.actor {
      onMessage(PrintMessage) {AsyncMessage _msg ->
        def msg = (PrintMessage) _msg
        println msg.value
        Thread.sleep(10L)
      }
    }

    def calculator = Actor.actor {
      onMessage(SqureMessage) {AsyncMessage _msg ->
        def msg = (SqureMessage) _msg
        msg.value * msg.value
      }
    }

    def results = Collections.synchronizedCollection([])

    def collector = Actor.actor {
      onMessage(CollectMessage) {AsyncMessage _msg ->
        results << ((CollectMessage) _msg).value
      }
    }

    Scheduler.sync {
      (0..999).each {int value ->
        calculator.post(new SqureMessage(value: value)) {def res ->
          printer.post(new PrintMessage(value: "${Thread.currentThread().id}: $value -> $res"))
          collector.post(new CollectMessage(value: value))
        }
      }
    }

    results.eachWithIndex {int it, int index ->
      printer.send(new PrintMessage(value: "$index $it"))
    }
  }
}

abstract class AsyncJob {
  Execution execution

  AsyncJob() {
  }

  AsyncJob(Execution exec) {
    execution = exec
  }

  public abstract void run()
}

@Use(ConcurrentLockCategory)
class Actor {
  Reaction reaction

  private LinkedBlockingQueue queue = new LinkedBlockingQueue()

  private ReadWriteLock lock = new ReentrantReadWriteLock()

  static Actor actor(Reaction reaction) {
    Actor mb = new Actor()
    Scheduler.async {
      mb.react(reaction)
    }
    mb
  }

  /**
   * Send synchroniously and wait for execution result
   */
  @Typed(debug = true)
  def send(AsyncMessage msg) {
    lock.writeLock().withLock {
      WorkerThread.setCurrentActor(msg.receiver)
      def result = reaction.handleMessage(msg)
      WorkerThread.setCurrentActor(null)
      result
    }
  }

  def send(Continuation run) {
    lock.writeLock().withLock {
      run.action(null)
    }
  }

  /**
   * Send asynchroniously
   */
  void post(AsyncMessage msg) {
    post(msg, null)
  }

  /**
   * Send asynchroniously with continuation
   */
  void post(AsyncMessage msg, Continuation whenDone) {
    msg.receiver = this
    msg.sender = WorkerThread.currentActor
    msg.whenDone = whenDone

    queue.offer msg

    Scheduler.async {
      lock.readLock().lock()

      def newMessage = (AsyncMessage) queue.take()

      if (newMessage) {
        def result = null
        try {
          WorkerThread.setCurrentActor(newMessage.receiver)
          result = newMessage.receiver.reaction.handleMessage(newMessage)
          WorkerThread.setCurrentActor(null)
        }
        finally {
          lock.readLock().unlock()
        }

        if (newMessage.whenDone) {
          WorkerThread.setCurrentActor(newMessage.sender)
          newMessage.whenDone.action result
          WorkerThread.setCurrentActor(null)
        }
      }
      else {
        lock.readLock().unlock()
      }
    }
  }

  void react(Reaction reaction) {
    this.reaction = reaction
    reaction.actor = this
    reaction.define()
  }
}

abstract class Reaction extends HashMap {
  Actor actor

  private AsyncHandler universal

  abstract void define()

  ;

  protected final void onMessage(Class type, AsyncHandler handler) {
    put(type, handler)
  }

  protected final def handleMessage(AsyncMessage msg) {
    def handler = (AsyncHandler) get(msg.class)
    handler = handler ?: universal
    handler?.handleMessage(msg)
  }

  void send(AsyncMessage msg) {
    actor.send msg
  }

  void post(AsyncMessage msg) {
    actor.post(msg)
  }

  void post(AsyncMessage msg, Continuation whenDone) {
    actor.post msg, whenDone
  }
}

class Execution {
  final CountDownLatch sync = new CountDownLatch(1)
  final AtomicInteger count = new AtomicInteger(1)

  Execution(Runnable run) {
    init(run)
  }

  private init(Runnable run) {
    Scheduler.async {
      execution = owner
      run.run()
      decrement()
    }
  }

  void increment() {
    count.incrementAndGet()
  }

  void decrement() {
    if (count.decrementAndGet() == 0) {
      sync.countDown()
    }
  }

  void await() {
    sync.await()
  }
}

interface Continuation {
  def action(Object data)
}

interface AsyncHandler {
  Object handleMessage(AsyncMessage msg)
}

class AsyncMessage {
  Actor sender
  Actor receiver
  Continuation whenDone
}

final class SqureMessage extends AsyncMessage {
  int value
}

final class CollectMessage extends AsyncMessage {
  int value
}

final class PrintMessage extends AsyncMessage {
  String value
}

class Scheduler {

  static final ArrayList threads = new ArrayList()

  static final Lock lock = new ReentrantLock()

  static final Random random = new Random()

  static void async(AsyncJob run) {
    def thread = Thread.currentThread()
    if (thread instanceof WorkerThread) {
      ((WorkerThread) thread).schedule run
    }
    else {
      startWorkers(run)
    }
  }

  static def sync(Runnable run) {
    def execution = new Execution(run)
    execution.await()
  }

  private static def startWorkers(AsyncJob run) {
    lock.lock()
    if (threads.size() == 0) {
      3.times {
        threads << new WorkerThread()
      }

      def newThread = new WorkerThread()
      newThread.schedule run
      threads.add(newThread)

      threads.each {
        ((WorkerThread) it).start()
      }
    }
    else {
      def index = random.nextInt(threads.size())
      ((WorkerThread) threads[index]).schedule run
    }
    lock.unlock()
  }

  static AsyncJob steel() {
    lock.lock()
    def index = random.nextInt(threads.size())
    def res = (AsyncJob) ((WorkerThread) threads[index]).queue.poll()
    lock.unlock()
    res
  }
}

class WorkerThread extends Thread {

  final static ThreadLocal current = new ThreadLocal()

  private AsyncJob currentJob

  private Actor currentA

  final LinkedBlockingQueue queue = new LinkedBlockingQueue()

  static Execution getCurrentExecution() {
    Thread thread = Thread.currentThread()
    if (thread instanceof WorkerThread && ((WorkerThread) thread).currentJob)
      ((WorkerThread) thread).currentJob.execution
    else
      null
  }

  static Actor getCurrentActor() {
    Thread thread = Thread.currentThread()
    if (thread instanceof WorkerThread && ((WorkerThread) thread).currentJob)
      ((WorkerThread) thread).currentA
    else
      current.get()
  }

  static void setCurrentActor(Actor actor) {
    Thread thread = Thread.currentThread()
    if (thread instanceof WorkerThread && ((WorkerThread) thread).currentJob)
      ((WorkerThread) thread).currentA = actor
    else
      current.set actor
  }

  void schedule(AsyncJob job) {
    if (!job.execution)
      job.execution = currentExecution

    if (job.execution)
      job.execution.increment()
    queue.offer job
  }

  void run() {
    while (true) {
      def job = (AsyncJob) queue.poll()
      if (!job)
        job = Scheduler.steel()

      if (job) {
        currentJob = job
        job.run()
        if (job.execution)
          job.execution.decrement()
        currentJob = null
      }
    }
  }
}

@Category(Lock)
public class ConcurrentLockCategory {

  /**
   * Execute given calculation under lock and returns calculated value
   */
  public <V> V withLock(Callable<V> action) {
    try {
      lock()
      action.call()
    }
    finally {
      unlock()
    }
  }
}
