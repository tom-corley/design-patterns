package behavioural.state;

public class Main {
    public static void main(String[] args) {
        Context context = new Context(new Pending());
        context.doAction();
        context.doAction();
        try {
            context.doAction();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }
}