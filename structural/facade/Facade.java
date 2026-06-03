package structural.facade;

public class Facade {
    private final ComplexSubsystem complexSubsystem;
    private final SecondarySubsystem secondarySubsystem;

    public Facade(ComplexSubsystem complexSubsystem, SecondarySubsystem secondarySubsystem) {
        this.complexSubsystem = complexSubsystem;
        this.secondarySubsystem = secondarySubsystem;
    }

    public void writeToFile(String filename, String content) {
        complexSubsystem.writeToFileAndClose(filename, content);
    }

    public void deleteFile(String filename) {
        secondarySubsystem.deleteFileAndClose(filename);
    }

    public void replaceFile(String filename, String newContent) {
        deleteFile(filename);
        writeToFile(filename, newContent);
    }
}