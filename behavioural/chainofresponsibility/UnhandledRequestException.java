package behavioural.chainofresponsibility;

public class UnhandledRequestException extends RuntimeException {
    public UnhandledRequestException(String message) {
        super(message);
    }
}
