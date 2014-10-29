package com.martinandersson.javaee.arquillian.persistence;

import javax.persistence.Embeddable;

/**
 * World's most simplest address.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Embeddable
public class Address
{
    private String street, city;
    
    
    
    public String getStreet() {
        return street;
    }
    
    public String getCity() {
        return city;
    }
    
    public Address setStreet(String street) {
        this.street = street;
        return this;
    }
    
    public Address setCity(String city) {
        this.city = city;
        return this;
    }
    
    
    
    /*
     *  ------------------
     * | OBJECT OVERRIDES |
     *  ------------------
     */
    
    @Override
    public String toString() {
        return new StringBuilder(Address.class.getSimpleName())
                .append("[")
                  .append("street=").append(street)
                  .append(", city=").append(city)
                .append("]")
                .toString();
    }
}