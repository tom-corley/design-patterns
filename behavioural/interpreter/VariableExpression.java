package behavioural.interpreter;

public class VariableExpression implements AbstractExpression {
    private final String name;

    public VariableExpression(String name) {
        this.name = name;
    }

    @Override
    public boolean interpret(Context context) {
        return context.getVariableValue(name);
    }
}