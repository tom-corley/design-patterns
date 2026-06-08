package behavioural.templatemethod;

public class PaymentService {
    private MethodTemplate methodTemplate;

    public PaymentService(MethodTemplate methodTemplate) {
        this.methodTemplate = methodTemplate;
    }

    public void processPayment(int paymentAmount) {
        methodTemplate.processPayment(paymentAmount);
    }
}
