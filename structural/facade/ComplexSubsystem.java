package structural.facade;

public class ComplexSubsystem {
    public void writeToFileAndClose(String filename, String content) {
        openFile(filename);
        writeToFile(filename, content);
        closeFile(filename);
    }

    private void openFile(String filename) {
        System.out.println("ComplexSubsystem: opening file " + filename);
    }

    private void writeToFile(String filename, String content) {
        System.out.println("ComplexSubsystem: writing to file " + filename);
        System.out.println("ComplexSubsystem: content: " + content);
    }

    private void closeFile(String filename) {
        System.out.println("ComplexSubsystem: closing file " + filename);
    }
}