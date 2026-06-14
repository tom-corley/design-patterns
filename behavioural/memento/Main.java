package behavioural.memento;

public class Main {
    public static void main(String[] args) {
        GameSave gameSave = new GameSave("GameState1");
        GameManager gameManager = new GameManager(gameSave);

        System.out.println(gameSave.getGameState());
        System.out.println(gameSave.getScore());
        System.out.println(gameSave.getHealth());

        gameManager.save();
        gameSave.takeDamage(10);
        gameSave.addScore(100);
        gameSave.heal(50);

        gameManager.save();
        gameManager.undo();
        gameManager.redo();

        System.out.println(gameSave.getGameState());
        System.out.println(gameSave.getScore());
        System.out.println(gameSave.getHealth());
    }
}