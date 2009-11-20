package groovy.util.concurrent

@Typed
class DivideAndConquerTest extends GroovyShellTestCase {
  // Test simple tree height calculation.
  class Node {
    List<Node> children

    def Node(children) {
      this.children = children
    }
  }

  def createTree(int height, int depth) {
    def children = (0 .. (int) (Math.random() * depth / height)).map { createTree(height+1, depth) }
    new Node(children)
  }

  def testTreeCreate() {
    assertNotNull createTree(1, 5)
  }

  SelfRecurringProblem calcHeightProblem(Node node) {
    [
       complex : { node.children.size() > 0 },
       sub     : { node.children.map { calcHeightProblem(it) } },
       solve   : { 1 },  // only called for leaves.
       combine : {results -> results.foldLeft(0){ int curr, int max -> Math.max(curr, max) } }
    ]
  }

  def testSolve() {
    def problem = calcHeightProblem(createTree(1, 5))
    int height = new DivideAndConquerProblemSolver(problem, 3).solve()
    assertTrue(height > 0)
  }
}
