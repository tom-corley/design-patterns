package structural.composite;

import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Component root = new InnerNode(new ArrayList<>(List.of(
            new Leaf("Leaf 1"),
            new Leaf("Leaf 2"),
            new InnerNode(new ArrayList<>(List.of(new Leaf("Leaf 3")))))));
        System.out.println(root.operation());
    }
}