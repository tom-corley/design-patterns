package structural.decorator;

public class ColourDecorator extends BaseHatDecorator {
    private final String colour;

    public ColourDecorator(String colour, Hat hat) {
        super(hat);
        this.colour = colour;
    }

    @Override
    public int getCost() {
        return 10 + hat.getCost();
    }

    @Override 
    public String toString() {
        return "Colourful " + colour + "\n" + hat.toString();
    }
}