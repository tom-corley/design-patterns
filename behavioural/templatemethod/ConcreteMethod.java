package behavioural.templatemethod;

public class ConcreteMethod extends MethodTemplate {
    @Override
    protected void validate(int paymentAmount) {
        System.out.println("Validating payment: " + paymentAmount);
    }

    @Override
    protected void debit(int paymentAmount) {
        System.out.println("Debiting payment: " + paymentAmount);
    }

    @Override
    protected void credit(int paymentAmount ) {
        System.out.println("Crediting payment: " + paymentAmount);
    }
}