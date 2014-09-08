/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server.db;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author oneadmin
 */
@Entity
public class Attribute {
 
	//For SQLite use GenerationType.AUTO to generate id
	//for derby, H2, MySQL etc use GenerationType.IDENTITY
 
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)  
	private Long id;
	private String name;
	private String phone;
	private double distance;
 
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
 
 
	//this is optional, just for print out into console
	@Override
	public String toString() {
		return "Rider [id=" + id + ", name=" + name + ", phone=" + phone + ", distance=" + distance + "]";
	}
 
}