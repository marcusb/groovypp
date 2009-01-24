package org.mbte.groovypp.typeinference

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Compile(debug = true)
public class ActorTest extends GroovyTestCase {
    void testActor () {
        def mb = new MailBox ()
        def done = new SyncVar()
        mb.loop {
            receive { int msg ->
                if (msg == -1) {
                  done.set (null)
                  breakLoop ()
                }

                msg*msg
            }
        }

        mb.submit {
            (0..10000).each { int value ->
                def res = mb.send(value)
                println "$value -> $res"
            }
            send(-1)
        }

        done.get ()
    }
}

@Compile(debug = true)
class MailBox {
    private final LinkedBlockingQueue queue = new LinkedBlockingQueue()

    private volatile boolean suspended;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Send synchroniously and wait for execution result
     */
    def send (def msg) {
        def res = new SyncVar()
        executor.submit {
            queue.offer(new MailBoxElement(message:msg, continuation:{ result -> res.set(result) }))
        }
        res.get ()
    }

    /**
     * Send asynchroniously
     */
    void post (def msg) {
        queue.offer(new MailBoxElement(message:msg))
    }

    /**
     * Define async reaction function for mailbox
     */
    protected final def react (Closure operation) {

    }

    /**
     * Loop given operation infinitely
     */
    protected final void loop (TypedClosure<MailBox> operation) {
        executor.submit {
//            try {
                operation.setDelegate(getOwner ())
                operation.call ()
//            }
//            catch(BreakControlException be) {
//                return
//            }
//            catch (Throwable t) {
//                t.printStackTrace()
//            }
            loop (operation)
        }
    }

    /**
     * Break loop execution
     */
    public final void breakLoop () {
        throw new BreakControlException();
    }

    /**
     * Submit operation for execution
     */
    protected final void submit (Closure operation) {
        executor.submit {
//            try {
                operation.setDelegate this 
                operation.call ()
//            }
//            catch (Throwable e) {
//                e.printStackTrace()
//            }
        }
    }

    /**
     * Receive message synchroniously
     */
    protected final def receive(TypedClosure<MailBox> operation) {
       MailBoxElement mbe = (MailBoxElement)queue.take ()
       operation.delegate = this
       def result = operation.call (mbe.message)
       if (mbe.continuation) {
           executor.submit {
               mbe.continuation.call (result)
           }
       }
    }
}

class MailBoxElement {
    def     message
    Closure continuation
}


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

class BreakControlException extends RuntimeException {
    BreakControlException () {
        super ()
    }
}