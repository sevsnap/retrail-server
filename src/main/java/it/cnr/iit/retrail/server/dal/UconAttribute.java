/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
    @ManyToMany(mappedBy = "attributes", fetch=FetchType.LAZY)
    protected final Collection<UconSession> sessions = new ArrayList<>();

    @ManyToOne//(fetch=FetchType.LAZY)
    protected UconAttribute parent;

    @OneToMany(mappedBy = "parent", fetch=FetchType.LAZY)//, cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    protected final Collection<UconAttribute> children = new ArrayList<>();

    @Index
    private String id;
    @Index
    private String category;

    private String type, value, issuer;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date expires;

    @Index
    private String factory;

    public static UconAttribute newInstance(PepAttributeInterface a) {
        UconAttribute attribute = new UconAttribute();
        attribute.id = a.getId();
        attribute.type = a.getType();
        attribute.value = a.getValue();
        attribute.issuer = a.getIssuer();
        attribute.category = a.getCategory();
        attribute.factory = a.getFactory();
        attribute.setExpires(a.getExpires());
        return attribute;
    }

    protected UconAttribute() {
        super();
    }

    public void copy(PepAttributeInterface pepAttribute) {
        id = pepAttribute.getId();
        type = pepAttribute.getType();
        value = pepAttribute.getValue();
        issuer = pepAttribute.getIssuer();
        category = pepAttribute.getCategory();
        expires = pepAttribute.getExpires();
        factory = pepAttribute.getFactory();
    }

    public Collection<UconSession> getSessions() {
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

    public UconAttribute getParent() {
        return parent;
    }

    public Collection<UconAttribute> getChildren() {
        return children;
    }

    //this is optional, just for print out into console
    @Override
    public String toString() {
        String s = getClass().getSimpleName() + " [rowId=" + rowId + ", id=" + id + "; value=" + value + ", #sessions=" + getSessions().size() + ", #children=" + getChildren().size() + ", factory=" + factory + "]";
        if (getParent() != null) {
            s += " *** with parent: " + getParent().toString();
        }
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UconAttribute)) {
            return false;
        }
        if (rowId == null || ((UconAttribute) o).rowId == null) {
            throw new RuntimeException("cannot compare " + this + " to " + o + " since they have no rowId");
        }
        return ((UconAttribute) o).rowId.equals(rowId);
    }

}
