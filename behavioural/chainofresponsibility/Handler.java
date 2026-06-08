package behavioural.chainofresponsibility;

public abstract class Handler {
    private Handler next;

    protected Handler(Handler next) {
        this.next = next;
    }

    protected abstract boolean canHandle(Request request);

    protected abstract void handle(Request request);

    public void handleOrForward(Request request) {
        if (canHandle(request)) {
            handle(request);
        } else if (next != null) {
            next.handleOrForward(request);
        } else {
            throw new UnhandledRequestException("No handler can handle the request");
        }
    }

    public void setNext(Handler next) {
        this.next = next;
    }

    public Handler getNext() {
        return next;
    }
}