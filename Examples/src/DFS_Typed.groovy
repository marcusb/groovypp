@Typed
package test

class DFST {
  private int visitedNodeCounter = 0

  private Function0 visitor = { -> visitedNodeCounter++ }

  def visit(Node node) {
    node.acceptVisitor visitor
    for(n in node.children) { visit(n) }
  }
}

class Node {
  ArrayList<Node> children = []

  def addChild(node) {
    children << node
  }

  def acceptVisitor(Function0 visitor) {
    visitor()
  }
}

def start = System.currentTimeMillis()
def root = new Node()

for(i in 0..<100) {
  root.addChild (new Node())

}

root.children.each { child ->
    for(i in 0..<50) {
        child.addChild (new Node())
    }
}

def dfs = new DFST()
dfs.visit(root)

println System.currentTimeMillis() - start
