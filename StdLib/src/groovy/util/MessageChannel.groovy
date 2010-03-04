package groovy.util

@Trait abstract class MessageChannel<T> {
    abstract MessageChannel<T> post (T message)

    MessageChannel<T> leftShift (T msg) {
        post msg
    }

    MessageChannel<T> addBefore(MessageChannel<T> other) {
        def that = this;
        { msg ->
            other.post(msg)
             that.post(msg)
        }
    }

    MessageChannel<T> addAfter(MessageChannel<T> other) {
        def that = this;
        { msg ->
             that.post(msg) 
            other.post(msg)
        }
    }

    /**
    * Convinience method to be create channel from closure
    */
//    static <T> MessageChannel<T> channel(MessageChannel<T> channel) {
//        channel
//    }
}