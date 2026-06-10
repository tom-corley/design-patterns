package behavioural.iterator;

public class Main {
    public static void main(String[] args) {
        Tree tree = new Tree(new Node(1, new Node(2, new Node(4), new Node(5)), new Node(3, new Node(6), new Node(7))));

        for (Node node : tree) {
            System.out.println(node.getValue());
        }
    }
}

/*
        1
       / \
      2   3
     / \ / \
    4  5 6  7
*/