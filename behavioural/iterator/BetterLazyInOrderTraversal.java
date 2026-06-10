package behavioural.iterator;

import java.util.Iterator;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.ArrayDeque;

public class BetterLazyInOrderTraversal implements Iterator<Node> {
    private final Deque<Node> stack = new ArrayDeque<>();

    public BetterLazyInOrderTraversal(Tree tree) {
        if (tree.getRoot() == null) {
            throw new IllegalArgumentException("Tree root cannot be null");
        }
        pushLeftTail(tree.getRoot());
    }

    private void pushLeftTail(Node node) {
        while (node != null) {
            stack.push(node);
            node = node.getLeft();
        }
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public Node next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Node node = stack.pop();
        pushLeftTail(node.getRight());
        return node;
    }
}