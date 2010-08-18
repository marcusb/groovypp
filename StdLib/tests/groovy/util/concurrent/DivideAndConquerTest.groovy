/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



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

  Node createTree(int height, int depth) {
    def nChildren = ((int) (Math.random() * depth / height))
    new Node((0..<nChildren).map { createTree(height + 1, depth) })
  }

  void testTreeCreate() {
    assertNotNull createTree(1, 5)
  }

  SelfRecurringProblem calcHeightProblem(Node node) {
    [
       complex : { node.children.size() > 0 },
       sub     : { node.children.map { calcHeightProblem(it) } },
       solve   : { 1 },  // only called for leaves.
       combine : { results -> results.foldLeft(0){ int curr, int max -> Math.max(curr, max) } + 1 }
    ]
  }

  int height(Node node) {
    if (node.children.isEmpty()) return 1;
    return 1 + node.children.map{ height(it) }.foldLeft(0) { int curr, int max -> Math.max(curr, max) }
  }

  void testSolve() {
    def node = createTree(1, 15)
    def problem = calcHeightProblem(node)
    int h = new DivideAndConquerProblemSolver(problem, 10).solve()
    assertEquals(height(node), h)
  }
}
