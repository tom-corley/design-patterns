package structural.decorator;

public abstract class BaseHatDecorator implements Hat {
    protected Hat hat;

    protected BaseHatDecorator(Hat hat) {
        if (hat == null) {
            throw new IllegalArgumentException("Hat cannot be null");
        }
        this.hat = hat;
    }

    @Override
    public void putOn() {
        hat.putOn();
    }

    @Override
    public void takeOff() {
        hat.takeOff();
    }

    @Override
    public int getCost() {
        return hat.getCost();
    }
}