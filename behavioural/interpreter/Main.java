package behavioural.interpreter;

public class Main {
    public static void main(String[] args) {
        Context context = new Context();
        context.setVariableValue("x", true);
        context.setVariableValue("y", false);

        AbstractExpression expression = new XorExpression(
            new ConstantExpression(true),
            new NonTerminalExpression(new VariableExpression("x"), new VariableExpression("y"))
        );

        System.out.println(expression.interpret(context));
    }
}