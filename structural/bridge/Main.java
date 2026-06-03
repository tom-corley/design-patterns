package structural.bridge;

public class Main {
    public static void main(String[] args) {
        Implementation email = new EmailImplementation();
        Implementation sms = new SMSImplementation();
        Abstraction promotionAlert = new PromotionAlert(email);
        Abstraction deliveryReminder = new DeliveryReminder(sms);
        promotionAlert.notify("10% off all products");
        deliveryReminder.notify("Your order will be delivered tomorrow");
    }
}