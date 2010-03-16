package shootout.mandelbrot
/* The Computer Language Benchmarks Game
   http://shootout.alioth.debian.org/

   contributed by Stefan Krause
   slightly modified by Chad Whipkey
   parallelized by Colin D Bennett 2008-10-04
   reduce synchronization cost by The Anh Tran
  */

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch

@Typed
class MandelbrotGroovy
{
    public static void main(String[] args) throws Exception
    {
        def size = 10000
        if (args.length >= 1)
            size = Integer.parseInt(args[0])

//        System.out.format("P4\n%d %d\n", size, size)
        long millis = System.currentTimeMillis()
        def width_bytes = size /8 + 1
        def output_data = new byte[size][width_bytes]
        def bytes_per_line = new int[size]

        compute(size, output_data, bytes_per_line)
	    long total = System.currentTimeMillis() - millis;
        println "[Mandelbrot-Groovy Benchmark Result: $total]"

/*
        BufferedOutputStream ostream = new BufferedOutputStream(System.out)
        for (i in 0..<size)
            ostream.write(output_data[i], 0, bytes_per_line[i])
        ostream.close()
*/
    }

    private static final void compute(final int N, final byte[][] output, final int[] bytes_per_line)
    {
        final def inverse_N = 2.0d / N
        final def current_line = new AtomicInteger(0)

        final def pool = new Thread[Runtime.getRuntime().availableProcessors()]
        def countDown = new CountDownLatch(pool.length)
        for (i in 0..<pool.length)
        {
            pool[i] = [
                run: {
                    int y
                    while ((y = current_line.getAndIncrement()) < N)
                    {
                        def pdata = output[y]

                        def bit_num = 0, byte_count = 0, byte_accumulate = 0

                        def Civ = y * inverse_N - 1.0d
                        for (x in 0..<N)
                        {
                            def Crv = (double)x * inverse_N - 1.5d

                            def Zrv = Crv, Ziv = Civ, Trv = Crv * Crv, Tiv = Civ * Civ

                            def j = 49
                            while (true)
                            {
                                Ziv = (Zrv * Ziv) + (Zrv * Ziv) + Civ
                                Zrv = Trv - Tiv + Crv

                                Trv = Zrv * Zrv
                                Tiv = Ziv * Ziv

                                if (((Trv + Tiv) > 4.0d) || (--j <= 0)) break
                            }

                            byte_accumulate <<= 1
                            if (j == 0)
                                byte_accumulate++

                            if (++bit_num == 8)
                            {
                                pdata[ byte_count++ ] = byte_accumulate
                                bit_num = byte_accumulate = 0
                            }
                        } // end foreach column

                        if (bit_num != 0)
                        {
                            byte_accumulate <<= (8 - (N & 7))
                            pdata[ byte_count++ ] = (byte)byte_accumulate
                        }

                        bytes_per_line[y] = byte_count
                    } // end while (y < N)
                    countDown.countDown()
                } // end void run()
            ]

            pool[i].start()
        }
        countDown.await()
    }
}