package behavioural.visitor;

public interface Visitor {
    void visit(Burger element);
    void visit(Spaghetti element);
}