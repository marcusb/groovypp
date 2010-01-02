class DFS {
  def visitedNodeCounter = 0

  def visitor = {
    visitedNodeCounter++
  }

  def visit(node) {
    node.acceptVisitor visitor
    node.children.each { visit(it) }
  }
}

class Node {
  def children = []

  def addChild(node) {
    children << node
  }

  def acceptVisitor(visitor) {
    visitor()
  }
}

def start = System.currentTimeMillis()

root = new Node()

for(i in 0..<100) {
  root.addChild (new Node())

}

root.children.each { child ->
    for(i in 0..<50) {
        child.addChild (new Node())
    }
}

dfs = new DFS()
dfs.visit(root)

result = dfs.visitedNodeCounter

println System.currentTimeMillis() - start
