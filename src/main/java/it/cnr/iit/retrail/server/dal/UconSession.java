/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.impl.PepSession;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import org.apache.commons.beanutils.BeanUtils;

import org.eclipse.persistence.annotations.Index;
import org.w3c.dom.Document;


/**
 *
 * @author oneadmin
 */
@Entity
public class UconSession implements PepSessionInterface {

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
    
    private Status status = Status.TRY;
    
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date lastSeen = new Date();

    @ManyToMany
    private Collection<Attribute> attributes;

    @Transient
    private URL uconUrl;
    
    static public UconSession newInstance(Document e) throws Exception {
        PepSession p = new PepSession(e);
        UconSession s = new UconSession();
        BeanUtils.copyProperties(s, p);
        return s;
    }

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
    
    @Override
    public String getCustomId() {
        return customId;
    }

    @Override
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

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public URL getUconUrl() {
        return uconUrl;
    }

    @Override
    public void setUconUrl(URL uconUrl) {
        this.uconUrl = uconUrl;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    //this is optional, just for print out into console
    @Override
    public String toString() {
        return "UconSession [uuid=" + uuid  + ", customId=" + customId +", status="+status+ ", pepUrl="+pepUrl+", lastSeen="+lastSeen+"]";
    }
}
