package behavioural.iterator;

import java.util.Iterator;

public class Tree implements Iterable<Node> {
    private Node root;

    public Tree(Node root) {
        this.root = root;
    }

    public Node getRoot() {
        return root;
    }

    @Override
    public Iterator<Node> iterator() {
        return new BetterLazyInOrderTraversal(this);
    }
}