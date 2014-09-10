/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.server.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import org.eclipse.persistence.annotations.Index;

/**
 *
 * @author oneadmin
 */
@Entity
public class UconSession implements Serializable {

    //For SQLite use GenerationType.AUTO to generate id
    //for derby, H2, MySQL etc use GenerationType.IDENTITY
    @Id
    //@GeneratedValue(strategy = GenerationType.AUTO)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Index(unique=true)
    private String cookie;
    
    private String pepUrl;

    @ManyToMany
    private Collection<Attribute> attributes;

    public Collection<Attribute> getAttributes() {
        return attributes;
    }

    public void addAttribute(Attribute attribute) {
        if(attributes == null)
            attributes = new HashSet<>();
        attributes.add(attribute);
        attribute.getSessions().add(this);
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPepUrl() {
        return pepUrl;
    }

    public void setPepUrl(String pepUrl) {
        this.pepUrl = pepUrl;
    }
    
    //this is optional, just for print out into console
    @Override
    public String toString() {
        return "UconSession [id=" + id + ", cookie=" + cookie + ", pepUrl="+pepUrl+"]";
    }

}
