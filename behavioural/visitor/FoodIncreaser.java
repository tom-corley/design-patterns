package behavioural.visitor;

public class FoodIncreaser implements Visitor {
    @Override
    public void visit(Burger element) {
        System.out.println("Adding another beef patty to the burger");
        element.addPatty();
    }
    @Override
    public void visit(Spaghetti element) {
        System.out.println("Adding another serving of spaghetti");
        element.addServing();
    }
}