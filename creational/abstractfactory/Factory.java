package creational.abstractfactory;

import creational.factory.BaseProduct;

public interface Factory {
    BaseProduct createProduct(String name, double price);
}