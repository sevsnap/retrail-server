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
import javax.persistence.Basic;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

    //For SQLite use GenerationType.AUTO to generate rowId
    //for derby, H2, MySQL etc use GenerationType.IDENTITY
    
    //@GeneratedValue(strategy = GenerationType.AUTO)
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @GeneratedValue(generator="system-uuid")
    private String uuid;
    
    @Index(unique=true)
    private String customId;
    
    private String pepUrl;
    
    private PepSession.Status status = PepSession.Status.TRY;
    
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
    
    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
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

    public String getUuid() {
        return uuid;
    }

    //this is optional, just for print out into console
    @Override
    public String toString() {
        return "UconSession [uuid=" + uuid  + ", customId=" + customId +", status="+status+ ", pepUrl="+pepUrl+", lastSeen="+lastSeen+"]";
    }

}
