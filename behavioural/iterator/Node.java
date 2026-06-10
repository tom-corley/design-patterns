package behavioural.iterator;

public class Node {
    private int value;
    private Node left;
    private Node right;

    public Node(int value) {
        this.value = value; // or constructor overloading
    }

    public Node(int value, Node left, Node right) {
        this.value = value;
        this.left = left;
        this.right = right;
    }

    public int getValue() {
        return value;
    }

    public Node getLeft() {
        return left;
    }

    public Node getRight() {
        return right;
    }
    
}