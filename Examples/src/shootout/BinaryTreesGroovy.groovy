package shootout;

@Typed
class BinaryTreesGroovy {
	private final static int minDepth = 4;

	public static void main(String[] args){
        long millis = System.currentTimeMillis ()

		int n = 0;

		if (args.length > 0) n = Integer.parseInt(args[0]);

		int maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n;
		int stretchDepth = maxDepth + 1;

		int check = TreeNode.bottomUpTree(0,stretchDepth).itemCheck();
		println("stretch tree of depth $stretchDepth\t check: $check");

		def longLivedTree = TreeNode.bottomUpTree(0,maxDepth);

		for (int depth=minDepth; depth<=maxDepth; depth+=2){
			int iterations = 1 << (maxDepth - depth + minDepth);
			check = 0;

			for (int i=1; i<=iterations; i++){
				check += TreeNode.bottomUpTree(i,depth).itemCheck();
				check += TreeNode.bottomUpTree(-i,depth).itemCheck();
			}
			println("${iterations*2}\t trees of depth $depth\t check: $check");
		}
		println("long lived tree of depth $maxDepth\t check: ${longLivedTree.itemCheck()}");

        println System.currentTimeMillis() - millis
	}
}

@Typed
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
        if (!left) return item;
        else return item + left.itemCheck() - right.itemCheck();
    }
}
