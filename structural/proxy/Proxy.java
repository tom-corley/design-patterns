package structural.proxy;

public class Proxy implements PaymentService {
    private final PaymentService delegate;
    private final String userType;

    public Proxy(PaymentService delegate, String userType) {
        this.delegate = delegate;
        this.userType = userType;
    }

    public void processPayment(int paymentAmount) {
        if (!userType.equals("admin")) {
            throw new IllegalArgumentException("User is not an admin");
        }
        delegate.processPayment(paymentAmount);
    }
}