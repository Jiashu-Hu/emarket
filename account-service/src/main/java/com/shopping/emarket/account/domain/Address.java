package com.shopping.emarket.account.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class Address {
    private String line1;
    private String line2;
    private String city;
    private String region;
    private String postalCode;
    private String country;

    protected Address() {}

    public Address(String line1, String line2, String city, String region, String postalCode, String country) {
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.region = region;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
}
