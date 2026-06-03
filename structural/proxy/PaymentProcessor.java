package structural.proxy;

public class PaymentProcessor implements PaymentService {
    public void processPayment(int paymentAmount) {
        System.out.println("Payment amount: " + (String.valueOf(paymentAmount)));
    }
}