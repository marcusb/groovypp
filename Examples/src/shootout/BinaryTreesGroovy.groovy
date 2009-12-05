package shootout

@Typed
class BinaryTreesGroovy {
	private final static int minDepth = 4

	public static void main(String[] args){

        long millis = System.currentTimeMillis ()

		int n = 0

		if (args.length > 0) n = Integer.parseInt(args[0])

		int maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n
		int stretchDepth = maxDepth + 1

		int check = TreeNode.bottomUpTree(0,stretchDepth).itemCheck()
		println("stretch tree of depth $stretchDepth\t check: $check")

		def longLivedTree = TreeNode.bottomUpTree(0,maxDepth)

		for (int depth=minDepth; depth<=maxDepth; depth+=2){
		    int iterations = 1 << (maxDepth - depth + minDepth)
		    check = (1..iterations).foldLeft(0) {i, int sum ->
              sum + TreeNode.bottomUpTree(i,depth).itemCheck() + TreeNode.bottomUpTree(-i,depth).itemCheck()}

            println("${iterations*2}\t trees of depth $depth\t check: $check")
		}
		println("long lived tree of depth $maxDepth\t check: ${longLivedTree.itemCheck()}")

        println System.currentTimeMillis() - millis
	}
}

@Typed
private static class TreeNode
{
    MetaClass getMetaClass () {}
    
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

    final int itemCheck(){
        this.@item + (this.@left ? this.@left.itemCheck() - this.@right.itemCheck() : 0);
    }
}
