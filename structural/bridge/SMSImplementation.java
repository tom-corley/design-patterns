package structural.bridge;

public class SMSImplementation implements Implementation {
    @Override
    public void sendMessage(String message) {
        System.out.println("Sending SMS: " + message);
    }
}
