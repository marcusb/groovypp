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

package groovy

class VArgsTest extends GroovyShellTestCase {

  void testPrimitiveMethod() {

    shell.evaluate(
            """
        @Typed
        def primitiveMethod(){0}

        @Typed
        def primitiveMethod(int i) {1}

        @Typed
        def primitiveMethod(int i, int j) {2}

        @Typed
        def primitiveMethod(int[] is) {10+is.length}

        @Typed
        def u() {
          assert primitiveMethod()==0
          assert primitiveMethod(1)==1
          assert primitiveMethod(1,1)==2
          assert primitiveMethod(1,1,1)==13
          assert primitiveMethod([1,2,2,2] as int[])==14
        }
        u()
      """
    );
  }



  void testDoubleMethod() {
    shell.evaluate(
            """
        @Typed
        def doubleMethod(double[] id) {20+id.length}

        @Typed
        def u() {
          // with BigDecimal
          assert doubleMethod([1,2,2,2] as BigDecimal[])==24
//          assert doubleMethod()==20
//          assert doubleMethod(1.0G)==21
//          assert doubleMethod(1.0G,1.0G)==22
//          assert doubleMethod(1.0G,1.0G,1.0G)==23
//
//          // with double
//          assert doubleMethod()==20
//          assert doubleMethod(1.0d)==21
//          assert doubleMethod(1.0d,1.0d)==22
//          assert doubleMethod(1.0d,1.0d,1.0d)==23
//          assert doubleMethod([1,2,2,2] as double[])==24
        }
        u()
      """
    );
  }

  // test vargs with one fixed argument for primitives
  void testDoubleMethodWithOneFixedPrimitive() {
    shell.evaluate(
            """
        @Typed
        def doubleMethod2(double a, double[] id) {31+id.length}

        @Typed
        def u() {
          // with BigDecimal
          assert doubleMethod2(1.0G)==31
          assert doubleMethod2(1.0G,1.0G)==32
          assert doubleMethod2(1.0G,1.0G,1.0G)==33
          assert doubleMethod2(1.0G, [1,2,2,2] as BigDecimal[])==35

          // with double
          assert doubleMethod2(1.0d)==31
          assert doubleMethod2(1.0d,1.0d)==32
          assert doubleMethod2(1.0d,1.0d,1.0d)==33
          assert doubleMethod2(1.0d,[1,2,2,2] as double[])==35
        }
        u()
      """
    );
  }




  void testObjectMethod() {
    shell.evaluate(
            """
        @Typed
        def objectMethod(){0}

        @Typed
        def objectMethod(Object i) {1}

        @Typed
        def objectMethod(Object i, Object j) {2}

        @Typed
        def objectMethod(Object[] is) {10+is.length}

        @Typed
        def u() {
          assert objectMethod()==0
          assert objectMethod(1)==1
          assert objectMethod(1,1)==2
          assert objectMethod(1,1,1)==13
          assert objectMethod([1,2,2,2] as Object[])==14
        }
        u()
      """
    );


  }

  void testStringVargsMethod() {
    shell.evaluate(
            """
        @Typed
        def gstringMethod(String[] gstrings){gstrings.length}

        @Typed
        def u() {
            def content = 1
            def gstring ="\$content"
            assert gstringMethod() == 0
            assert gstringMethod(gstring) == 1
            assert gstringMethod(gstring,gstring,gstring) == 3
            assert gstringMethod([gstring] as String[]) == 1
        }

        u()
      """
    );

  }

  void testStringMethod() {
    def script = """
        @Typed
        def stringMethod(String[] strings) {strings.length}

        @Typed
        def u() {
          def content = 1
          def gstring ="\$content"
          assert stringMethod() == 0
          assert stringMethod(gstring) == 1
          assert stringMethod(gstring,gstring,gstring) == 3
          assert stringMethod([gstring] as String[]) == 1
          assert stringMethod() == 0
          assert stringMethod("a") == 1
          assert stringMethod("a","a","a") == 3
          assert stringMethod(["a"] as String[]) == 1
        }

        u()
      """
    shell.evaluate(script);

  }

  //tests related to GROOVY-1807
  void testOverloadedMethod1() {
    shell.evaluate(
            """
        @Typed
        def overloadedMethod1(String s){1}

        @Typed
        def overloadedMethod1(Object[] args){2}

        @Typed
        def u() {
          assert overloadedMethod1() == 2
        }
      """
    );

  }

  void testOverloadedMethod2() {
    shell.evaluate(
            """
        @Typed
        def overloadedMethod2(x,y){1}

        @Typed
        def overloadedMethod2(x,Object... y){2}

        @Typed
        def u() {
          assert overloadedMethod2(null) == 2
          assert overloadedMethod2("foo") == 2
        }

        u()
      """
    );
  }



  void testArrayCoercion() {
    shell.evaluate(
            """
        @Typed
        def normalVargsMethod(Object[] a){a.length}

        @Typed
        def u() {
          assert normalVargsMethod([1,2,3] as int[]) == 3
        }

		u()
      """
    );

  }

  // GROOVY-2204
  void test2204a() {
    shell.evaluate(
            """
        @Typed
        def m2204a(Map kwargs=[:], arg1, arg2, Object[] args) {
          "arg1: \$arg1, arg2: \$arg2, args: \$args, kwargs: \$kwargs"
        }

        @Typed      
        def m2204b(Map kwargs=[:], arg1, arg2="1", Object[] args) {
          "arg1: \$arg1, arg2: \$arg2, args: \$args, kwargs: \$kwargs"
        }

        @Typed
        def u() {
           assert m2204a('hello', 'world') == 'arg1: hello, arg2: world, args: [], kwargs: [:]'
           assert m2204a('hello', 'world', 'from', 'list') == 'arg1: hello, arg2: world, args: [from, list], kwargs: [:]'
           assert m2204a('hello', 'world', 'from', 'list', from: 'kwargs') == 'arg1: hello, arg2: world, args: [from, list], kwargs: [from:kwargs]'
           assert m2204a('hello', 'world', from: 'kwargs') == 'arg1: hello, arg2: world, args: [], kwargs: [from:kwargs]'
           assert m2204a([:], 'hello', 'world', [] as Object[]) == 'arg1: hello, arg2: world, args: [], kwargs: [:]'

           assert m2204b('hello', 'world') == 'arg1: hello, arg2: 1, args: [world], kwargs: [:]'
           assert m2204b('hello', 'world', 'from', 'list') == 'arg1: hello, arg2: 1, args: [world, from, list], kwargs: [:]'
           assert m2204b('hello', 'world', 'from', 'list', from: 'kwargs') == 'arg1: hello, arg2: world, args: [from, list], kwargs: [from:kwargs]'
           assert m2204b('hello', 'world', from: 'kwargs') == 'arg1: hello, arg2: world, args: [], kwargs: [from:kwargs]'
        }

		u()
      """
    );
  }

  // GROOVY-2351


  void test2351() {
    shell.evaluate(
            """
        @Typed
        def m2351(Object... args)  {1}

        @Typed
        def m2351(Integer... args) {2}

        @Typed
        def u() {
          assert m2351(1, 2, 3, 4, 5) == 2
        }
		u()
      """
    );

  }

  // see MetaClassHelper#calculateParameterDistance
  void testAB() {
    shell.evaluate(
            """
        @Typed
        def fooAB(Object[] a) {1}     //-> case B

        @Typed
        def fooAB(a,b,Object[] c) {2} //-> case A

        @Typed
        def u() {
          assert fooAB(new Object(),new Object()) == 2
        }
		u()
      """
    );

  }

  void testAC() {
    shell.evaluate(
            """
        @Typed
        def fooAC(Object[] a) {1}     //-> case B

        @Typed
        def fooAC(a,b)        {2}     //-> case C

        @Typed
        def u() {
          assert fooAC(new Object(),new Object()) == 2
        }

		u()
      """
    );
  }

  void testAD() {
    shell.evaluate(
            """
        @Typed
        def fooAD(Object[] a) {1}     //-> case D

        @Typed
        def fooAD(a,Object[] b) {2}   //-> case A

        @Typed
        def u() {
          assert fooAD(new Object()) == 2
        }
		u()
      """
    );

  }

  void testBC() {
    shell.evaluate(
            """
        @Typed
        def fooBC(Object[] a) {1}     //-> case B

        @Typed
        def fooBC(a,b) {2}            //-> case C

        @Typed
        def u() {
          assert fooBC(new Object(),new Object()) == 2
        }

		u()
      """
    );

  }

  void testBD() {
    shell.evaluate(
            """
        @Typed
        def fooBD(Object[] a)   {1}   //-> case B

        @Typed
        def fooBD(a,Object[] b) {2}   //-> case D

        @Typed
        def u() {
          assert fooBD(new Object(),new Object()) == 2
        }
		u()
      """
    );

  }

  // GROOVY-3019
  void test3019() {
    shell.evaluate(
            """
        @Typed
        def foo3019(Object a, int b) {1}

        @Typed
        def foo3019(Integer a, int b, Object[] arr) {2}

        @Typed
        def u() {
          assert foo3019(new Integer(1),1)==1
        }

		u()
      """
    );

  }
}  
