package behavioural.chainofresponsibility;

public class Main {
    public static void main(String[] args) {
        Handler handler = new PrimaryHandler(new SecondaryHandler(new TertiaryHandler(null)));

        Request smallRequest = new Request(50);
        Request mediumRequest = new Request(500);
        Request largeRequest = new Request(5000);

        handler.handleOrForward(smallRequest);
        handler.handleOrForward(mediumRequest);
        handler.handleOrForward(largeRequest);
    }
}