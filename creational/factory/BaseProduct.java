package creational.factory;

public abstract class BaseProduct {
    private String name;
    private double price;

    protected BaseProduct(String name, double price) {
        this.name = name;
        this.price = price;
    }
    
    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }
    
    public void printProduct() {
        System.out.println("Product: " + name + ", Price: " + price);
    }
}