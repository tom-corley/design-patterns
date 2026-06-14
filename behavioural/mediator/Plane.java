package behavioural.mediator;

public class Plane extends Colleague {
    public Plane(Mediator mediator) {
        super(mediator);
    }

    public void requestLanding() {
        notify("requestLanding");
    }

    public void land(String landing) {
        System.out.println(landing);
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            notify("Landed Successfully");
        }
    }
}