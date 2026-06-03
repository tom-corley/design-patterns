package structural.composite;

public class Leaf implements Component {
    private final String name;

    public Leaf(String name) {
        this.name = name;
    }

    @Override
    public String operation() {
        return name;
    }

    @Override
    public void add(Component component) {
        throw new UnsupportedOperationException("Leaf cannot add children");
    }

    @Override
    public void remove(Component component) {
        throw new UnsupportedOperationException("Leaf cannot remove children");
    }
}