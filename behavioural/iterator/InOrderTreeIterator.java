package behavioural.iterator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public class InOrderTreeIterator implements Iterator<Node> {
    private Deque<Node> stack = new ArrayDeque<>();
    private final List<Node> traversal = new ArrayList<>();
    private int index = 0;

    public InOrderTreeIterator(Tree tree) {
        if (tree.getRoot() == null) {
            throw new IllegalArgumentException("Tree root cannot be null");
        }
        addToTraversal(tree.getRoot());
    }

    private void addToTraversal(Node node) {
        if (node.getLeft() != null) {
            addToTraversal(node.getLeft());
        }
        traversal.add(node);
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

// pre-order traversal
// - visit the current node
// - visit the left subtree
// - visit the right subtree
// in-order traversal
// - visit the left subtree
// - visit the current node
// - visit the right subtree
// post-order traversal
// - visit the left subtree
// - visit the right subtree
// - visit the current node