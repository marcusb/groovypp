@Typed(debug=true) package shootout

class BinaryTreesGroovy {
	private final static int minDepth = 4

	public static void main(String[] args){

        long millis = System.currentTimeMillis ()

		int n = 0

		if (args.length > 0) n = Integer.parseInt(args[0])

		int maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n
		int stretchDepth = maxDepth + 1

		int check = TreeNode.bottomUpTree(0,stretchDepth).itemCheck()
		System.out.println("stretch tree of depth $stretchDepth\t check: $check")

		TreeNode longLivedTree = TreeNode.bottomUpTree(0,maxDepth)

		for (int depth=minDepth; depth<=maxDepth; depth+=2){
            TreeNode.iterateDepth(maxDepth, depth)
		}
		System.out.println("long lived tree of depth $maxDepth\t check: ${longLivedTree.itemCheck()}")

        System.out.println System.currentTimeMillis() - millis
	}

    private static class TreeNode
    {
        private TreeNode left, right
        private int item

        TreeNode(int item){
            this.item = item;
        }

        TreeNode(TreeNode left, TreeNode right, int item){
            this.left = left;
            this.right = right;
            this.item = item;
        }

        private static TreeNode bottomUpTree(int item, int depth){
            depth ? [bottomUpTree(2*item-1, depth-1), bottomUpTree(2*item, depth-1), item] : [item]
        }

        private int itemCheck(){
            left ? item + left.itemCheck() - right.itemCheck() : item
        }

        static iterateDepth (int maxDepth, int depth) {
            int iterations = 1 << (maxDepth - depth + minDepth)
            int check = 0
            for (int i=1; i<=iterations; i++){
                check += bottomUpTree(i,depth).itemCheck() +  bottomUpTree(-i,depth).itemCheck()
            }

            System.out.println("${iterations*2}\t trees of depth $depth\t check: $check")
        }
    }
}
