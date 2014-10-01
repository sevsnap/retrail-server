/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepRequestAttribute;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import org.eclipse.persistence.annotations.Index;


/**
 *
 * @author oneadmin
 */

@Entity
public class Attribute implements Serializable {

	//For SQLite use GenerationType.AUTO to generate id
    //for derby, H2, MySQL etc use GenerationType.IDENTITY
    @Id
    //@GeneratedValue(strategy = GenerationType.AUTO)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rowId;
    @ManyToMany(mappedBy="attributes")
    private Collection<UconSession> sessions;

    @ManyToOne
    private Attribute parent;

    @OneToMany(mappedBy="parent")
    private Collection<Attribute> children;

    @Index
    private String id;
    @Index
    private String category;
    
    private String type, value, issuer;
    
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date expires;
    
    private String factory;

    public static Attribute newInstance(PepRequestAttribute pepAttribute, Attribute parent) {
        Attribute attribute = new Attribute();
        attribute.id = pepAttribute.id;
        attribute.type = pepAttribute.type;
        attribute.value = pepAttribute.value;
        attribute.issuer = pepAttribute.issuer;
        attribute.category = pepAttribute.category;
        attribute.expires = pepAttribute.expires;
        attribute.factory = pepAttribute.factory;
        attribute.children = new ArrayList<>();
        attribute.setParent(parent);
        return attribute;
    }
    
    private Attribute() {
        super();
    }
    
    public void copy(PepRequestAttribute pepAttribute, Attribute parent) {
        type = pepAttribute.type;
        value = pepAttribute.value;
        issuer = pepAttribute.issuer;
        category = pepAttribute.category;
        expires = pepAttribute.expires;        
        factory = pepAttribute.factory;
        setParent(parent);
    }
    
    public Collection<UconSession> getSessions() {
        if(sessions == null)
            sessions = new HashSet<>();
        return sessions;
    }

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

    public Long getRowId() {
        return rowId;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }
    
    public Attribute getParent() {
        return parent;
    }
    
    public void setParent(Attribute parent) {
        if(this.parent != null)
            this.parent.children.remove(this.parent);
        if(parent != null) {
            this.parent = parent;
            parent.children.add(parent);
        }
    }

    public Collection<Attribute> getChildren() {
        return children;
    }
    
    //this is optional, just for print out into console
    @Override
    public String toString() {
        return "Attribute [rowId="+rowId+", sessions="+sessions.size()+", id=" + id + ", type=" + type + ", value=" + value + ", issuer=" + issuer + ", category=" + category + "; factory="+factory+"]";
    }

}
