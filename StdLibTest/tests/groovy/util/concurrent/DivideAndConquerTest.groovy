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
}
