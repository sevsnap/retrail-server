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
    private Long key;
    private String id, type, value, issuer, category;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getkey() {
        return key;
    }

    public void setKey(Long key) {
        this.key = key;
    }

    //this is optional, just for print out into console
    @Override
    public String toString() {
        return "Attribute [id=" + id + ", type=" + type + ", value=" + value + ", issuer=" + issuer + ", category=" + category + "]";
    }

}
