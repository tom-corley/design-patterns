package structural.bridge;

public class EmailImplementation implements Implementation {
    @Override
    public void sendMessage(String message) {
        System.out.println("Sending email: " + message);
    }
}
