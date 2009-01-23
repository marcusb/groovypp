package org.mbte.groovypp.typeinference

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

@Compile
public class ActorTest extends GroovyTestCase {
    void testActor () {
        def printer = MailBox.actor {
            onMessage(PrintMessage) { MailBoxMessage _msg ->
                def msg = (PrintMessage)_msg
                println msg.value
            }
        }

        def calculator = MailBox.actor {
            onMessage(SqureMessage) { MailBoxMessage _msg ->
                def msg = (SqureMessage)_msg
                msg.value*msg.value
            }
        }

        (1..10000).each { int value ->
            calculator.post(new SqureMessage(value:value)) { def res ->
                printer.post ( new PrintMessage(value:"${Thread.currentThread().id}: $value -> $res"), null )
            }
        }

        calculator.send (new StopMessage())
        printer.send (new StopMessage())
    }
}

@Compile
class MailBox {
    Reaction reaction

    private final LinkedBlockingQueue queue = new LinkedBlockingQueue()

    public static MailBox actor (Reaction reaction) {
        MailBox mb = new MailBox()
        Scheduler.schedule {
            mb.react (reaction)
        }
        mb
    }

    /**
     * Send synchroniously and wait for execution result
     */
    def send (MailBoxMessage msg) {
        def res = new SyncVar()
        post(msg, {def result -> res.set(result)})
        res.get ()
    }

    /**
     * Send asynchroniously
     */
    void post (MailBoxMessage msg) {
        post (msg, null)
    }

    /**
     * Send asynchroniously with continuation
     */
    void post (MailBoxMessage msg, MessageContinuation whenDone) {
        msg.receiver = this
        msg.sender = WorkerThread.currentActor
        msg.whenDone = whenDone

        queue.offer msg
        
        Scheduler.schedule {
            def newMessage = (MailBoxMessage)queue.take()

            WorkerThread.setCurrentActor(msg.receiver)
            newMessage.handleMessage()
            WorkerThread.setCurrentActor(null)
        }
    }

    void react(Reaction reaction) {
        this.reaction = reaction
        reaction.mailBox = this
        reaction.define()
    }
}

@Compile
abstract class Reaction extends HashMap {
    MailBox mailBox

    private MessageHandler universal

    abstract void define ();

    protected final void onMessage(Class type, MessageHandler handler) {
          put(type,handler)
    }

    protected final def handleMessage (MailBoxMessage msg) {
        def handler = (MessageHandler) get(msg.class)
        handler = handler ? handler : universal
        handler ? handler.handleMessage(msg) : null
    }

    void send(MailBoxMessage msg) {
        mailBox.send msg
    }

    void post (MailBoxMessage msg) {
        mailBox.post(msg)
    }

    void post (MailBoxMessage msg, MessageContinuation whenDone) {
        mailBox.post msg, whenDone
    }
}

interface MessageContinuation {
    void onMessageResult (Object result)
}

interface MessageHandler {
    Object handleMessage (MailBoxMessage msg)
}

class MailBoxMessage {
    MessageContinuation whenDone
    MailBox             sender
    MailBox             receiver

    final def handleMessage () {
        def result = receiver.reaction.handleMessage(this)
        if (whenDone) {
            whenDone.onMessageResult result
        }
        result
    }
}

final class StartMessage extends MailBoxMessage {}

final class StopMessage extends MailBoxMessage {}

final class SqureMessage extends MailBoxMessage {int value}

final class PrintMessage extends MailBoxMessage {String value}

@Compile
class SyncVar<T> extends CountDownLatch {
    private T value

    SyncVar () {
        super (1)
    }

    T get () {
        await ()
        value
    }

    void set (T v) {
        value = v
        countDown()
    }
}

@Compile
class Scheduler {
    static final ArrayList threads = new ArrayList()

    static final Lock lock = new ReentrantLock ()

    static final Random random = new Random ()

    static void schedule (Runnable run) {
        def thread = Thread.currentThread()
        if (thread instanceof WorkerThread) {
            ((WorkerThread)thread).schedule run
        }
        else {
            lock.lock ()
            if (threads.size () == 0) {
                3.times {
                    threads << new WorkerThread()
                }

                def newThread = new WorkerThread()
                newThread.schedule run
                threads.add(newThread)

                threads.each {
                    ((WorkerThread)it).start ()
                }
            }
            else {
                def index = random.nextInt (threads.size())
                ((WorkerThread)threads [index]).schedule run
            }
            lock.unlock()
        }
    }

    static void loop () {
        Thread thread = Thread.currentThread()
        if (thread instanceof WorkerThread && ((WorkerThread)thread).currentRunnable)
            schedule (((WorkerThread)thread).currentRunnable)
        else
            throw new IllegalArgumentException ()
    }

    static Runnable steel() {
        lock.lock ()
        def index = random.nextInt (threads.size())
        def res = (Runnable)((WorkerThread)threads [index]).queue.poll()
        lock.unlock()
        res
    }
}

@Compile
class WorkerThread extends Thread {
    private MailBox currentMailBox

    Runnable currentRunnable

    final LinkedBlockingQueue queue = new LinkedBlockingQueue()

    static MailBox getCurrentActor () {
        Thread thread = Thread.currentThread()
        (thread instanceof WorkerThread) ? ((WorkerThread)thread).currentMailBox : null
    }

    static void setCurrentActor (MailBox mailBox) {
        Thread thread = Thread.currentThread()
        if (thread instanceof WorkerThread)
            ((WorkerThread)thread).currentMailBox = mailBox;
        else
            throw new IllegalArgumentException ()
    }

    void schedule (Runnable r) {
        queue.offer r
    }

    void run () {
        while(true) {
            def run = (Runnable)queue.poll()
            if (!run)
               run = Scheduler.steel ()
            if (run)
               run.run ()
        }
    }
}
