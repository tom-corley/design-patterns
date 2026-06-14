package behavioural.visitor;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Element> elements = new ArrayList<>(List.of(new Burger(2), new Spaghetti(2)));
        List<Visitor> visitors = new ArrayList<>(List.of(new Eater(), new FoodIncreaser()));

        for (Element element : elements) {
            for (Visitor visitor : visitors) {
                element.accept(visitor);
            }
        }
    }
}