package behavioural.mediator;

import java.util.ArrayList;
import java.util.List;

public class TrafficController implements Mediator {
    private final List<Plane> planes = new ArrayList<>();
    private int availableRunways;

    public TrafficController(int availableRunways) {
        this.availableRunways = availableRunways;
    }

    public void notify(Colleague sender, String event) {
        if ("requestLanding".equals(event)) {
            if (availableRunways > 0) {
                ((Plane) sender).land("Landing approved");
                availableRunways--;
            } else {
                ((Plane) sender).land("Landing denied, no available runways");
            }
        }

        if ("Landed Successfully".equals(event)) {
            availableRunways++;
        }
    }
}