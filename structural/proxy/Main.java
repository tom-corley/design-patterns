package structural.proxy;

public class Main {
    public static void main(String[] args) {
        PaymentProcessor paymentProcessor = new PaymentProcessor();
        PaymentService paymentService = new Proxy(paymentProcessor, "admin");
        paymentService.processPayment(100);

        paymentService = new Proxy(paymentProcessor, "user");
        try {
            paymentService.processPayment(100);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}