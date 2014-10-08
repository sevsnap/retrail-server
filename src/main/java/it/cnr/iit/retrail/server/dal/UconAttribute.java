/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
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
public class UconAttribute implements PepAttributeInterface {

	//For SQLite use GenerationType.AUTO to generate id
    //for derby, H2, MySQL etc use GenerationType.IDENTITY
    @Id
    //@GeneratedValue(strategy = GenerationType.AUTO)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rowId;
    @ManyToMany(mappedBy="attributes")
    private Collection<UconSession> sessions;

    @ManyToOne
    private UconAttribute parent;

    @OneToMany(mappedBy="parent")
    private Collection<UconAttribute> children;

    @Index
    private String id;
    @Index
    private String category;
    
    private String type, value, issuer;
    
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date expires;
    
    @Index
    private String factory;

    public static UconAttribute newInstance(PepAttributeInterface a, UconAttribute parent) {
        UconAttribute attribute = new UconAttribute();
        attribute.id = a.getId();
        attribute.type = a.getType();
        attribute.value = a.getValue();
        attribute.issuer = a.getIssuer();
        attribute.category = a.getCategory();
        attribute.factory = a.getFactory();
        attribute.setExpires(a.getExpires());
        attribute.children = new ArrayList<>();
        attribute.setParent(parent);
        return attribute;
    }

    protected UconAttribute() {
        super();
    }
    
    public void copy(PepAttributeInterface pepAttribute, UconAttribute parent) {
        id = pepAttribute.getId();
        type = pepAttribute.getType();
        value = pepAttribute.getValue();
        issuer = pepAttribute.getIssuer();
        category = pepAttribute.getCategory();
        expires = pepAttribute.getExpires();        
        factory = pepAttribute.getFactory();
        setParent(parent);
    }
    
    public Collection<UconSession> getSessions() {
        if(sessions == null)
            sessions = new HashSet<>();
        return sessions;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getRowId() {
        return rowId;
    }

    @Override
    public Date getExpires() {
        return expires;
    }

    @Override
    public void setExpires(Date expires) {
        this.expires = expires;
    }

    @Override
    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }
    
    @Override
    public PepAttributeInterface getParent() {
        return parent;
    }
    
    @Override
    public void setParent(PepAttributeInterface parent) {
        if(this.parent != null)
            this.parent.children.remove(this.parent);
        if(parent != null) {
            this.parent = (UconAttribute)parent;
            ((UconAttribute)parent).getChildren().add((UconAttribute)parent);
        }
    }

    public Collection<UconAttribute> getChildren() {
        if(children == null)
            children = new ArrayList<>();
        return children;
    }
    
    //this is optional, just for print out into console
    @Override
    public String toString() {
        return getClass().getSimpleName()+" [rowId="+rowId+", sessions="+getSessions().size()+", id=" + id + ", type=" + type + ", value=" + value + ", issuer=" + issuer + ", category=" + category + "; factory="+factory+"]";
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof UconAttribute && Objects.equals(((UconAttribute)o).rowId, rowId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.rowId);
        return hash;
    }

}
