package creational.builder;

public class Builder {
    private String name;
    private String address;
    private String city;
    private String state;
    private String zip;
    private int numberOfRooms;

    public Builder() {
        this.name = "Default Name";
        this.address = "Default Address";
        this.city = "Default City";
        this.state = "Default State";
        this.zip = "Default Zip";
        this.numberOfRooms = 0;
    }
    
    public Builder setName(String name) {
        this.name = name;
        return this;
    }

    public Builder setAddress(String address) {
        this.address = address;
        return this;
    }
    
    public Builder setCity(String city) {
        this.city = city;
        return this;
    }

    public Builder setState(String state) {
        this.state = state;
        return this;
    }
    
    public Builder setZip(String zip) {
        this.zip = zip;
        return this;
    }

    public Builder addBathroom() {
        this.numberOfRooms++;
        return this;
    }

    public Builder addBedroom() {
        this.numberOfRooms++;
        return this;
    }

    public Builder addLivingRoom() {
        this.numberOfRooms++;
        return this;
    }

    public Builder addKitchen() {
        this.numberOfRooms++;
        return this;
    }
    
    public House build() {
        return new House(name, address, city, state, zip, numberOfRooms);
    }
}