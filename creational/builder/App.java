package creational.builder;

public class App {
    public static void main(String[] args) {
        Builder builder = new Builder();
        House house = builder.addBathroom()
          .addBedroom()
          .addLivingRoom()
          .addKitchen()
          .setName("House 1")
          .setAddress("123 Main St")
          .setCity("Anytown")
          .setState("CA")
          .setZip("12345")
          .build();
        house.printHouse();
    }
}