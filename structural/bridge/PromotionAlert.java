package structural.bridge;

public class PromotionAlert extends Abstraction {
    public PromotionAlert(Implementation implementation) {
        super(implementation);
    }

    @Override
    public void notify(String message) {
        String formattedMessage = "This is a promotion alert: " + message;
        implementation.sendMessage(formattedMessage);
    }
}
