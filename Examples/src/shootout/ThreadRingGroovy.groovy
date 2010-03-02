package shootout;

/**
 * The Computer Language Benchmarks Game
 * http://shootout.alioth.debian.org/
 *
 * contributed by Fabien Le Floc'h
 *
 * Java implementation of thread-ring benchmark. Best performance is achieved with
 * MAX_THREAD=1 as the thread-ring test is bested with only 1 os thread.
 * This implementation shows using a simple thread pool solves the thread context
 * switch issue.
 */

import java.util.concurrent.*;

@Typed
class ThreadRingGroovy {
    private static final int MAX_NODES = 503;
    private static final int MAX_THREADS = 503;

    private ExecutorService executor;
    private int N;

    static final CountDownLatch cdl = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        def start = System.currentTimeMillis();
        def n = Integer.parseInt(args[0]);
        def ring = new ThreadRingGroovy(n);
        def node = ring.start(MAX_NODES);
        node.sendMessage(new TokenMessage(1,0));
        cdl.await();
        long end = System.currentTimeMillis() - start
        System.out.println("$n:$end")
    }

    public ThreadRingGroovy(int n) {
        N = n;
    }

    public Node start(int n) {
        def nodes = spawnNodes(n);
        connectNodes(n, nodes);
        return nodes[0];
    }

    private Node[] spawnNodes(int n) {
        executor = Executors.newFixedThreadPool(MAX_THREADS);
        Node[] nodes = new Node[n+1];
        for (int i = 0; i < n ; i++) {
            nodes[i] = new Node(i+1, null);
        }
        return nodes;
    }

    public void connectNodes(int n, Node[] nodes) {
        nodes[n] = nodes[0];
        for (i in 0..<n) {
            nodes[i].connect(nodes[i+1]);
        }
    }

    private static class TokenMessage {
        int nodeId;
        volatile int value;
        boolean isStop;

        public TokenMessage(int nodeId, int value) {
            this.nodeId = nodeId
            this.value = value
        }

        public TokenMessage(int nodeId, int value, boolean isStop) {
            this.nodeId = nodeId
            this.value = value
            this.isStop = isStop
        }
    }

    private class Node implements Runnable {
        private int nodeId
        private Node nextNode
        private LinkedBlockingQueue<TokenMessage> queue = []
        private boolean isActive
        private int counter

        public Node(int id, Node nextNode) {
            this.nodeId = id
            this.nextNode = nextNode
            this.counter = 0
        }

        public void connect(Node node) {
            this.nextNode = node;
            isActive = true;
        }

        public void sendMessage(TokenMessage m) {
            queue.add(m);
            executor.execute(this);
        }


        public void run() {
            if (isActive) {
                try {
                    TokenMessage m = queue.take();
                    if (m.isStop) {
                        int nextValue = m.value+1;
                        if (nextValue == MAX_NODES) {
                            executor.shutdown();
                            cdl.countDown();
                        } else {
                            m.value = nextValue;
                            nextNode.sendMessage(m);
                        }
                        isActive = false;
                    } else {
                        if (m.value == N) {
                            System.out.println(nodeId);
                            nextNode.sendMessage(new TokenMessage(nodeId, 0, true));
                        } else {
                            m.value = m.value + 1;
                            nextNode.sendMessage(m);
                        }
                    }
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }
}