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
            [left : bottomUpTree(2*item-1, depth-1), right : bottomUpTree(2*item, depth-1), item : item]
        }
        else {
            [item : item]
        }
    }

    int itemCheck(){
        // if necessary deallocate here
        if (left) return item;
        else return item + left.itemCheck() - right.itemCheck();
    }
}

TreeNode.bottomUpTree(0,5)
        """
    }
}