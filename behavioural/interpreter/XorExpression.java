package behavioural.interpreter;

public class XorExpression implements AbstractExpression {
    private final AbstractExpression left;
    private final AbstractExpression right;

    public XorExpression(AbstractExpression left, AbstractExpression right) {
        this.left = left;
        this.right = right;
    }
    
    @Override
    public boolean interpret(Context context) {
        return left.interpret(context) ^ right.interpret(context);
    }
}