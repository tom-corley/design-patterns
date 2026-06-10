package behavioural.iterator;

import java.util.Iterator;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.ArrayDeque;
import java.util.HashSet;

public class LazyInOrderTraversal implements Iterator<Node> {
    private Deque<Node> stack = new ArrayDeque<>();
    private HashSet<Node> visited = new HashSet<>();

    public LazyInOrderTraversal(Tree tree) {
        if (tree.getRoot() == null) {
            throw new IllegalArgumentException("Tree root cannot be null");
        }
        stack.push(tree.getRoot());
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
        // Get the leftmost node
        Node node = stack.peek();
        while (node.getLeft() != null && !visited.contains(node.getLeft())) {
            stack.push(node.getLeft());
            node = node.getLeft();
        }
        node = stack.pop();
        visited.add(node);
        if (node.getRight() != null && !visited.contains(node.getRight())) {
            stack.push(node.getRight());
        }
        return node;
    }
}