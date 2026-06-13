package behavioural.interpreter;

class ConstantExpression implements AbstractExpression {

    private final Boolean value;

    public ConstantExpression(Boolean value) {
        this.value = value;
    }

    @Override
    public boolean interpret(Context context) {
        return value;
    }
}