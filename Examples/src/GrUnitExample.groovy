@GrUnit({
    @Field Calculator calc
    setUp { calc = [] }
})
@Typed
class Calculator {
    private ArrayList<Double> list = []

    @GrUnit({ assertEquals(10d, calc.push(10d).list[-1]) })
    Calculator push(double v) {
        list.push(v)
        this
    }

    @GrUnit({
        shouldFail { calc.peek() }
        assertEquals(10d, calc.push(10d).peek())
        assertEquals(1, calc.list.size())
    })
    double peek() {
        list[-1]
    }

    @GrUnit({
        testFailIfEmpty{ shouldFail { calc.pop() } }
        testPushPop {
            assertEquals(10d, calc.push(10d).pop())
            assertEquals(0, calc.list.size())
        }
    })
    double pop() {
        list.pop()
    }

    @GrUnit({
        assertEquals (4d, calc.push(4d).peek())
        testPlusNull{ assertEquals (10d, calc.push(6).plus().peek()) }
        testPlusNonNull {assertEquals (10d, calc.plus(6).peek())}
    })
    Calculator plus(Double value = null) {
        push((value == null ? pop () : value) + pop ())
    }
}
