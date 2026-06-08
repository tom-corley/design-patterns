package behavioural.chainofresponsibility;

public class TertiaryHandler extends Handler {
    public TertiaryHandler(Handler next) {
        super(next);
    }

    @Override
    public boolean canHandle(Request request) {
        return request.getRequestSize() < 10000;
    }

    @Override
    public void handle(Request request) {
        System.out.println("TertiaryHandler: Handling request of size " + request.getRequestSize());
    }
}