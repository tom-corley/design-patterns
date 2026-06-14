package behavioural.visitor;

public class Eater implements Visitor {
    @Override
    public void visit(Burger element) {
        System.out.println("Picking up the burger, and taking a bite");
    }
    @Override
    public void visit(Spaghetti element) {
        System.out.println("Twirling the spaghetti around my fork, and taking a bite");
    }
}