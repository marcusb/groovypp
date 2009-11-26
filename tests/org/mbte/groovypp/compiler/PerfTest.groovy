package org.mbte.groovypp.compiler

public class PerfTest extends GroovyShellTestCase {

  void testPerf() {
    def res = shell.parse("""
          double normalMethod(long count) {
             double sum = 0d
             while(--count != 0){
               sum = 5l + sum / 3f + count * 2d
             }
             sum
          }

          @Typed
          double fastMethod(long count) {
             double sum = 0d
             while(--count != 0){
               sum = 5l + sum / 3f + count * 2d
             }
             sum
          }

          @Typed
          double fastMethodWithInference(long count) {
             def sum = 0d
             while(--count != 0){
               sum = (5l + sum / 3f + count * 2d)
             }
             sum
          }
    """
    )

    for (int i = 2000000; i != 2000010; i++) {
      Thread.sleep 0L

      long start = System.currentTimeMillis()
      def nsum = res.normalMethod(i)
      long t1 = System.currentTimeMillis() - start
      print "normal: $t1"

      Thread.sleep 0L

      start = System.currentTimeMillis()
      def fsum = res.fastMethod(i)
      long t2 = System.currentTimeMillis() - start
      print " fast: $t2 ${1d * t1 / t2}"

      Thread.sleep 0L

      start = System.currentTimeMillis()
      def fisum = res.fastMethodWithInference(i)
      long ti2 = System.currentTimeMillis() - start
      println " fastInference: $ti2 ${1d * t1 / ti2}"

      assertEquals nsum, fsum
    }
  }
}