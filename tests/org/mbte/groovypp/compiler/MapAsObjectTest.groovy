package org.mbte.groovypp.compiler

public class MapAsObjectTest extends GroovyShellTestCase {

    void testMe () {
        def res = shell.evaluate ("""
            @Trait
            abstract class Function1<T,R> {
                abstract R apply (T param)

                R getAt (T arg) {
                    apply(arg)
                }
            }

            @Typed(debug=true)
            static <T,R> Iterator<R> map (Iterator<T> self, Function1<T,R> op) {
                [next: { op [ self.next() ] }, hasNext: { self.hasNext() }, remove: { self.remove() } ]
            }

            @Typed(debug=true)
            def u () {
                def res = []

                def newIt = map([1,2,3,4].iterator ()) {
                    it + 10
                }

                while (newIt.hasNext ())
                    res << (newIt.next () + 1)

                res
            }

            u ()
        """)

       assertEquals ([12, 13, 14, 15], res) 
    }

    void testTree () {
        shell.evaluate """
@Typed(debug=true)
class TreeNode
{
    TreeNode left, right;
    int item;

    static TreeNode bottomUpTree(int item, int depth){
        if (depth>0){
            [
              left: bottomUpTree(2*item-1, depth-1),
              right: bottomUpTree(2*item, depth-1),
              item: item
            ]
        }
        else {
            [item : item];
        }
    }

    int itemCheck(){
			this.@item + (this.@left ? this.@left.itemCheck() - this.@right.itemCheck() : 0);
    }

	private final static int minDepth = 4

	public static void main(String[] args){

        long millis = System.currentTimeMillis ()

		int n = 0

		if (args.length > 0) n = Integer.parseInt(args[0])

		int maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n
		int stretchDepth = maxDepth + 1

		int check = TreeNode.bottomUpTree(0,stretchDepth).itemCheck()
		println("stretch tree of depth \$stretchDepth\t check: \$check")

		def longLivedTree = TreeNode.bottomUpTree(0,maxDepth)

		for (int depth=minDepth; depth<=maxDepth; depth+=2){
			int iterations = 1 << (maxDepth - depth + minDepth)
			check = 0

            int i = 1
			while (i <= iterations){
				check = check + TreeNode.bottomUpTree(i,depth).itemCheck() + TreeNode.bottomUpTree(-i,depth).itemCheck()
                i++
			}
			println("\${iterations*2}\t trees of depth \$depth\t check: \$check")
		}
		println("long lived tree of depth \$maxDepth\t check: \${longLivedTree.itemCheck()}")

        println System.currentTimeMillis() - millis
	}
}

null
        """
    }
}