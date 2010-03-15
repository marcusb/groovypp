package groovy.util.concurrent

/*
class SampleFStruct implements FStruct {
    private int balance

    SampleFStruct(SampleFStruct copyFrom) {
        this.balance = copyFrom.balance
    }

    int getBalance () {
        balance
    }

    Mutable makeMutable () {
        new Mutable(this)
    }

    Mutable withBalance(int balance) {
        new Mutable(this).withBalance(balance)
    }

    SampleFStruct mutate(Function1<Mutable,?> op) {
        def copy = makeMutable()
        op(copy)
        copy.makeImmutable()
    }

    static class Mutable extends SampleFStruct {
        Mutable (SampleFStruct copyFrom) {
            super(copyFrom)
        }

        Mutable withBalance(int balance) {
            this.balance = balance
            this
        }

        SampleFStruct makeImmutable () {
            new SampleFStruct(this)
        }
    }
}
*/