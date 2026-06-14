package behavioural.memento;

public class GameSave {
    private String gameState;
    private int score;
    private int health;

    public GameSave(String gameState) {
        this.gameState = gameState;
        this.score = 0;
        this.health = 100;
    }
    
    public void takeDamage(int damage) {
        health -= damage;
    }

    public void addScore(int score) {
        this.score += score;
    }

    public void heal(int amount) {
        health += amount;
    }

    public String getGameState() {
        return gameState;
    }

    public int getScore() {
        return score;
    }

    public int getHealth() {
        return health;
    }

    public GameSaveMemento createMemento() {
        return new GameSaveMemento(gameState, score, health);
    }

    public void restore(GameSaveMemento memento) {
        this.gameState = memento.gameState;
        this.score = memento.score;
        this.health = memento.health;
    }

    public static class GameSaveMemento {
        private final String gameState;
        private final int score;
        private final int health;

        public GameSaveMemento(String gameState, int score, int health) {
            this.gameState = gameState;
            this.score = score;
            this.health = health;
        }
    }
}