package structural.facade;

public class Main {
    public static void main(String[] args) {
        ComplexSubsystem complexSubsystem = new ComplexSubsystem();
        SecondarySubsystem secondarySubsystem = new SecondarySubsystem();
        Facade facade = new Facade(complexSubsystem, secondarySubsystem);
        facade.writeToFile("test.txt", "Hello, world!");
        facade.replaceFile("test.txt", "Hello, universe!");
        facade.deleteFile("test.txt");
    }
}