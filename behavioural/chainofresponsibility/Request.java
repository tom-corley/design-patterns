package behavioural.chainofresponsibility;

public class Request {
    private final int requestSize;

    public Request(int requestSize) {
        this.requestSize = requestSize;
    }

    public int getRequestSize() {
        return requestSize;
    }
}
