package behavioural.mediator;

public class Main {
    public static void main(String[] args) {
        TrafficController trafficController = new TrafficController(3);
        Plane plane1 = new Plane(trafficController);
        Plane plane2 = new Plane(trafficController);
        Plane plane3 = new Plane(trafficController);
        Plane plane4 = new Plane(trafficController);

        plane1.requestLanding();
        plane2.requestLanding();
        plane3.requestLanding();  
        plane4.requestLanding();
    }
}