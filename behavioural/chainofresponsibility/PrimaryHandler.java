package behavioural.chainofresponsibility;

public class PrimaryHandler extends Handler {
    public PrimaryHandler(Handler next) {
        super(next);
    }

    @Override
    public boolean canHandle(Request request) {
        return request.getRequestSize() < 100;
    }

    @Override
    public void handle(Request request) {
        System.out.println("PrimaryHandler: Handling request of size " + request.getRequestSize());
    }
}