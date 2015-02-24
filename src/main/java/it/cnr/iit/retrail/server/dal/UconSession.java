/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.impl.PepSession;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Column;
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
public class UconSession extends PepSession {

    //For SQLite use GenerationType.AUTO to generate rowId
    //for derby, H2, MySQL etc use GenerationType.IDENTITY
    //@GeneratedValue(strategy = GenerationType.AUTO)
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @GeneratedValue(generator = "system-uuid")
    private String uuid;

    @Index(unique = true)
    private String customId;

    private String pepUrl;

    private Status status = Status.BEGIN;

    @Column(nullable = false)
    private String stateName;

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date lastSeen = new Date();

    @ManyToMany
    private Collection<UconAttribute> attributes = new ArrayList<>();

    @Transient
    private URL uconUrl;

    @Transient
    private long ms;

    public UconSession() throws Exception {
        super();
    }

    static public UconSession newInstance(Document e) throws Exception {
        PepSession p = new PepSession(e);
        UconSession s = new UconSession();
        // Note: a simple copyProperties will not copy the localInfo map.
        BeanUtils.copyProperties(s, p);
        return s;
    }

    public UconSession(Document doc) throws Exception {
        super(doc);
    }

    public Collection<UconAttribute> getAttributes() {
        return attributes;
    }

    public void addAttribute(UconAttribute attribute) {
        attributes.add(attribute);
        attribute.getSessions().add(this);
    }

    public void removeAttribute(UconAttribute attribute) {
        attributes.remove(attribute);
        attribute.getSessions().remove(this);
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

    public void setStatus(String statusString) { // FIXME
        this.status = Status.valueOf(statusString);
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
    public long getMs() {
        return ms;
    }

    @Override
    public void setMs(long ms) {
        this.ms = ms;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UconSession && Objects.equals(((UconSession) o).uuid, uuid);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.uuid);
        return hash;
    }

    //this is optional, just for print out into console
    @Override
    public String toString() {
        return "UconSession [uuid=" + uuid + ", customId=" + customId + ", status=" + status + ", pepUrl=" + pepUrl + ", lastSeen=" + lastSeen + "]";
    }
}
