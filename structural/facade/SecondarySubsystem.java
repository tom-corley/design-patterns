package structural.facade;

public class SecondarySubsystem {
    public void deleteFile(String filename) {
        System.out.println("SecondarySubsystem: deleting file " + filename);
    }

    public void deleteFileAndClose(String filename) {
        deleteFile(filename);
        closeFile(filename);
    }

    private void closeFile(String filename) {
        System.out.println("SecondarySubsystem: closing file " + filename);
    }
}
