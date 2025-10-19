package org.surino.untraceable.model;


import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Status status = Status.SCONOSCIUTO; 
    
    private String name;
    private String surname;
    private String address;

    public Person() {}
    public Person(String name, String surname) {
        this(name, surname, "");
    }


	public Person(String name, String surname, String address) {
		super();
		this.name = name;
		this.surname = surname;
		this.address = address;
	}
	
	
	
	
	public Person(String name, String surname, String address, Status status) {
		super();
		this.name = name;
		this.surname = surname;
		this.address = address;
		this.status = status;
	}
	
	
	
	
	public Person(String name, String surname, Status status) {
		this.name = name;
		this.surname = surname;
		this.address = "";
		this.status = status;
	}
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the surname
	 */
	public String getSurname() {
		return surname;
	}
	/**
	 * @param surname the surname to set
	 */
	public void setSurname(String surname) {
		this.surname = surname;
	}
	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(Status status) {
		this.status = status;
	}
	
	
	

}
