package behavioural.visitor;

public class Spaghetti implements Element {
    private int numberOfServings;

    public Spaghetti(int numberOfServings) {
        this.numberOfServings = numberOfServings;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void addServing() {
        this.numberOfServings++;
    }
}