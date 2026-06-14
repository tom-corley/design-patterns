package behavioural.visitor;

public class Burger implements Element {
    private int numberOfPatties;

    public Burger(int numberOfPatties) {
        this.numberOfPatties = numberOfPatties;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
    
    public void addPatty() {
        this.numberOfPatties++;
    }
}