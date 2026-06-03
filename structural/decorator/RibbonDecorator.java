package structural.decorator;

public class RibbonDecorator extends BaseHatDecorator {
    public RibbonDecorator(Hat hat) {
        super(hat);
    }

    @Override
    public int getCost() {
        return 10 + hat.getCost();
    }

    @Override
    public String toString() {
        return "Ribbon \n" + hat.toString();
    }
}