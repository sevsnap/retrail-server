/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.server.db;

import it.cnr.iit.retrail.commons.PepSession;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Temporal;
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
    private String cookie = randomId();
    
    private String pepUrl;
    
    PepSession.Status status = PepSession.Status.TRY;
    
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date lastSeen = new Date();

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

    public void removeAttribute(Attribute attribute) {
        if(attributes != null) {
            attributes.remove(attribute);
            attribute.getSessions().remove(this);
        }
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

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public PepSession.Status getStatus() {
        return status;
    }

    public void setStatus(PepSession.Status status) {
        this.status = status;
    }
    
    //this is optional, just for print out into console
    @Override
    public String toString() {
        return "UconSession [id=" + id + ", cookie=" + cookie + ", pepUrl="+pepUrl+", lastSeen="+lastSeen+"]";
    }
    
    private static String randomId() {
        char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

}
