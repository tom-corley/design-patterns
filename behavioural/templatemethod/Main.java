package behavioural.templatemethod;

public class Main {
    public static void main(String[] args) {
        PaymentService paymentService = new PaymentService(new ConcreteMethod());
        paymentService.processPayment(100);
        paymentService.processPayment(200);
        paymentService.processPayment(300);
    }
}