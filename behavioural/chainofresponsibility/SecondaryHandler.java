package behavioural.chainofresponsibility;

public class SecondaryHandler extends Handler {
    public SecondaryHandler(Handler next) {
        super(next);
    }

    @Override
    public boolean canHandle(Request request) {
        return request.getRequestSize() < 1000;
    }

    @Override
    public void handle(Request request) {
        System.out.println("SecondaryHandler: Handling request of size " + request.getRequestSize());
    }
}