package behavioural.templatemethod;

public abstract class MethodTemplate {
    public final void processPayment(int paymentAmount) {
        validate(paymentAmount);
        debit(paymentAmount);
        credit(paymentAmount);
    }

    protected abstract void validate(int paymentAmount);
    protected abstract void debit(int paymentAmount);
    protected abstract void credit(int paymentAmount);
}