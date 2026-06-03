package structural.decorator;

public class Main {
    public static void main(String[] args) {
        Hat redRibbonTrilby = 
        new ColourDecorator("Red",(
            new RibbonDecorator(
                new Trilby()
            )
        ));
        System.out.println(redRibbonTrilby);
        System.out.println("Cost: " + redRibbonTrilby.getCost());
    }
}