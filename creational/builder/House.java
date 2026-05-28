package creational.builder;

public class House {
    private String name;
    private String address;
    private String city;
    private String state;
    private String zip;
    private final int numberOfRooms;

    public House(String name, String address, String city, String state, String zip, int numberOfRooms) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.numberOfRooms = numberOfRooms;
    }

    public void printHouse() {
        System.out.println("House: " + name + ", " + address + ", " + city + ", " + state + ", " + zip + ", " + numberOfRooms);
    }
}