package structural.bridge;

public class DeliveryReminder extends Abstraction {
    public DeliveryReminder(Implementation implementation) {
        super(implementation);
    }

    @Override 
    public void notify(String message) {
        String formattedMessage = "This is a delivery reminder: " + message;
        implementation.sendMessage(formattedMessage);
    }
}
