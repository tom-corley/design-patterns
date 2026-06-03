package structural.composite;

public interface Component {
    void add(Component component);
    void remove(Component component);
    String operation();
}