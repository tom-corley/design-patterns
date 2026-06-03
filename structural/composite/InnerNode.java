package structural.composite;

import java.util.List;

public class InnerNode implements Component {
    private final List<Component> children;

    public InnerNode(List<Component> children) {
        this.children = children;
    }

    @Override
    public void add(Component component) {
        children.add(component);
    }

    @Override
    public void remove(Component component) {
        children.remove(component);
    }

    @Override
    public String operation() {
        StringBuilder sb = new StringBuilder();
        for (Component child : children) {
            sb.append(child.operation());
            sb.append(" ");
        }
        return sb.toString();
    }
}