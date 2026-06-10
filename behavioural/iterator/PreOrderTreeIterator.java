package behavioural.iterator;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public class PreOrderTreeIterator implements Iterator<Node> {
    private final List<Node> traversal = new ArrayList<>();
    private int index = 0;

    public PreOrderTreeIterator(Tree tree) {
        if (tree.getRoot() == null) {
            throw new IllegalArgumentException("Tree root cannot be null");
        }
        addToTraversal(tree.getRoot());
    }

    private void addToTraversal(Node node) {
        traversal.add(node);
        if (node.getLeft() != null) {
            addToTraversal(node.getLeft());
        }
        if (node.getRight() != null) {
            addToTraversal(node.getRight());
        }
    }

    @Override
    public boolean hasNext() {
        return index < traversal.size();
    }

    @Override
    public Node next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return traversal.get(index++);
    }
}
