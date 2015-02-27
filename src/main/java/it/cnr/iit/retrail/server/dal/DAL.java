/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.persistence.config.PersistenceUnitProperties;

/**
 *
 * @author oneadmin
 */
public class DAL implements DALInterface {

    //Create entity manager, this step will connect to database, please check 
    //JDBC driver on classpath, jdbc URL, jdbc driver name on persistence.xml
    private static final String PERSISTENCE_UNIT_NAME = "retrail";
    private static EntityManagerFactory factory;

    protected static final Logger log = LoggerFactory.getLogger(DAL.class);
    final private ThreadLocal entityManager;

    private static DAL instance;

    public static DAL getInstance() {
        if (instance == null) {
            if (factory == null) {
                Map<String, String> properties = new HashMap<>();
                properties.put(PersistenceUnitProperties.WEAVING, "dynamic");
                properties.put(PersistenceUnitProperties.DDL_GENERATION, "create-tables");
                properties.put(PersistenceUnitProperties.DDL_GENERATION_MODE, "database");
                properties.put(PersistenceUnitProperties.DDL_GENERATION_INDEX_FOREIGN_KEYS, "true");
                properties.put(PersistenceUnitProperties.SESSION_CUSTOMIZER, UUIDSequence.class.getCanonicalName());
                factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);
            }
            instance = new DAL();
        }
        return instance;
    }

    public void begin() {
        EntityManager em = (EntityManager) entityManager.get();
        // clearing the entity manager is fundamental to avoid stale objects that
        // may have been updated by other threads!
        em.clear();
        em.getTransaction().begin();
    }

    public void commit() {
        EntityManager em = (EntityManager) entityManager.get();
        em.getTransaction().commit();
    }

    public void rollback() {
        EntityManager em = (EntityManager) entityManager.get();
        em.getTransaction().rollback();
    }

    public void debugDump() {
        log.info("current open sessions:");
        Collection<UconSession> q = listSessions();
        for (UconSession s : q) {
            log.info("\t" + s);
            for (UconAttribute a : s.attributes) {
                log.info("\t\t" + a);
            }
        }
        //System.out.println("DAL: all attributes:");
        //for (UconAttribute a: listAttributesByFactoryUUID())
        //        System.out.println("\t" + a);
    }

    public void debugDumpAttributes(Collection<UconAttribute> al) {
        log.info("debugging attributes");
        for (UconAttribute a : al) {
            UconAttribute parent = (UconAttribute) a.getParent();
            log.info("\t{} (parent: {})", a, parent == null ? null : parent.getRowId());
            for (UconSession s : a.getSessions()) {
                log.info("\t\t{}", s);
            }
        }
    }

    private DAL() {
        log.info("creating Data Access Layer");

        this.entityManager = new ThreadLocal() {
            @Override
            protected synchronized Object initialValue() {
                try {
                    return factory.createEntityManager();
                } catch (Exception e) {
                    log.error("while creating thread local entity manager: {}", e);
                    throw e;
                }
            }
        };
        debugDump();
    }

    @Override
    public Collection<UconSession> listSessions() {
        EntityManager em = (EntityManager) entityManager.get();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s",
                UconSession.class);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listSessions(Status status) {
        EntityManager em = (EntityManager) entityManager.get();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.status = :status",
                UconSession.class)
                .setParameter("status", status);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listSessions(Date lastSeenBefore) {
        EntityManager em = (EntityManager) entityManager.get();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.lastSeen < :lastSeen",
                UconSession.class)
                .setParameter("lastSeen", lastSeenBefore);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listSessions(URL pepUrl) {
        EntityManager em = (EntityManager) entityManager.get();
        String url = pepUrl.toString();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.pepUrl = :pepUrl",
                UconSession.class)
                .setParameter("pepUrl", url);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listOutdatedSessions() {
        EntityManager em = (EntityManager) entityManager.get();
        TypedQuery<UconSession> q = em.createQuery(
                "select distinct s from UconSession s, UconAttribute a where s.status = :status and s member of a.sessions and a.expires < :now",
                UconSession.class)
                .setParameter("status", Status.ONGOING)
                .setParameter("now", new Date());
        Collection<UconSession> involvedSessions = q.getResultList();
        return involvedSessions;
    }

    @Override
    public Collection<UconAttribute> listAttributes(URL pepUrl) {
        EntityManager em = (EntityManager) entityManager.get();
        TypedQuery<UconAttribute> q = em.createQuery(
                "select distinct a from UconAttribute a, UconSession s where s.pepUrl = :pepUrl and s member of a.sessions",
                UconAttribute.class)
                .setParameter("pepUrl", pepUrl.toString());
        Collection<UconAttribute> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<UconAttribute> listAttributes() {
        EntityManager em = (EntityManager) entityManager.get();
        TypedQuery<UconAttribute> q = em.createQuery(
                "select a from UconAttribute a",
                UconAttribute.class);
        Collection<UconAttribute> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<UconAttribute> listAttributes(String category, String id) {
        EntityManager em = (EntityManager) entityManager.get();
        TypedQuery<UconAttribute> q = em.createQuery(
                "select a from UconAttribute a where a.category = :category and a.id = :id",
                UconAttribute.class)
                .setParameter("category", category)
                .setParameter("id", id);
        Collection<UconAttribute> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<PepAttributeInterface> listManagedAttributes(String factory) {
        EntityManager em = (EntityManager) entityManager.get();
        TypedQuery<PepAttributeInterface> q = em.createQuery(
                "select a from UconAttribute a where a.factory = :factory and a.expires is null",
                PepAttributeInterface.class)
                .setParameter("factory", factory);
        Collection<PepAttributeInterface> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<PepAttributeInterface> listUnmanagedAttributes(String factory) {
        EntityManager em = (EntityManager) entityManager.get();
        TypedQuery<PepAttributeInterface> q = em.createQuery(
                "select a from UconAttribute a where a.factory = :factory and a.expires is not null",
                PepAttributeInterface.class)
                .setParameter("factory", factory);
        Collection<PepAttributeInterface> attributes = q.getResultList();
        return attributes;
    }

    private UconSession saveSession(EntityManager em, UconSession uconSession, UconRequest uconRequest) {
        // Store request's attributes to the database
        // put parents to front since we have a CASCADE towards children
        LinkedList<UconAttribute> rootsFirst = new LinkedList<>();
        for (PepAttributeInterface pepAttribute : uconRequest) {
            UconAttribute uconAttribute = (UconAttribute) pepAttribute;
            if (uconAttribute.getParent() == null) {
                rootsFirst.addFirst((UconAttribute) pepAttribute);
            } else {
                rootsFirst.addLast((UconAttribute) pepAttribute);
            }
        }
        // Then, process all attributes (persist, or merge).
        // Also, keep merged the original uconRequest for consistency.
        uconSession = em.merge(uconSession);
        uconSession.attributes.clear();
        for (UconAttribute uconAttribute : rootsFirst) {
            if (uconAttribute.getRowId() == null) {
                em.persist(uconAttribute);
            }
            int index = 0;
            while (uconAttribute != uconRequest.get(index)) {
                index++;
            }
            uconAttribute = em.merge(uconAttribute);
            uconRequest.set(index, uconAttribute);
            uconAttribute.sessions.remove(uconSession);
            uconAttribute.sessions.add(uconSession);
            uconSession.attributes.add(uconAttribute);
        }
        uconSession = em.merge(uconSession);
        return uconSession;
    }

    @Override
    public UconSession startSession(UconSession uconSession, UconRequest uconRequest) throws Exception {
        // Store request's attributes to the database
        EntityManager em = (EntityManager) entityManager.get();
        try {
            em.persist(uconSession);
            uconSession = saveSession(em, uconSession, uconRequest);
        } catch (Exception e) {
            log.error("while creating dal session: {}", e);
            throw e;
        }
        return uconSession;
    }

    @Override
    public UconSession saveSession(UconSession uconSession, UconRequest uconRequest) {
        // Store request's attributes to the database
        log.debug("saving " + uconSession);
        EntityManager em = (EntityManager) entityManager.get();
        try {
            uconSession = saveSession(em, uconSession, uconRequest);
        } catch (Exception e) {
            log.error("while saving dal session: {}", e.getMessage());
            throw e;
        }
        return uconSession;
    }

    @Override
    public UconSession getSession(String uuid, URL uconUrl) {
        // TODO use custom generated value
        EntityManager em = (EntityManager) entityManager.get();
        UconSession uconSession = em.find(UconSession.class, uuid);
        if (uconSession == null) {
            throw new RuntimeException("Session with uuid=" + uuid + " is unknown");
        }
        uconSession.setUconUrl(uconUrl);
        return uconSession;
    }

    @Deprecated
    @Override
    public UconSession getSessionByCustomId(String customId) {
        EntityManager em = (EntityManager) entityManager.get();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.customId = :id",
                UconSession.class)
                .setParameter("id", customId);
        UconSession uconSession = q.getSingleResult();
        return uconSession;
    }

    @Override
    public UconAttribute getSharedAttribute(String category, String id) {
        EntityManager em = (EntityManager) entityManager.get();
        UconAttribute uconAttribute;
        try {
            TypedQuery<UconAttribute> q = em.createQuery(
                    "select a from UconAttribute a where a.category = :category and a.id = :id and a.parent is null",
                    UconAttribute.class)
                    .setParameter("category", category)
                    .setParameter("id", id);
            uconAttribute = q.getSingleResult();
        } catch (NoResultException e) {
            uconAttribute = null;
        }
        return uconAttribute;
    }

    @Override
    public void removeAttributesByFactory(String factory) {
        log.debug("removing all attributes for " + factory);
        EntityManager em = (EntityManager) entityManager.get();
        try {
            TypedQuery<UconAttribute> q = em.createQuery(
                    "select a from UconAttribute a where a.factory = :factory",
                    UconAttribute.class)
                    .setParameter("factory", factory);
            for (UconAttribute a : q.getResultList()) {
                Collection<UconSession> l = new ArrayList<>(a.sessions);
                if (a.parent != null) {
                    a.parent.children.remove(a);
                }
                a.parent = null;
                for (UconSession s : l) {
                    s.attributes.remove(a);
                }
                em.remove(a);
            }
        } catch (Exception e) {
            log.error("while removing attributes: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public UconSession endSession(UconSession uconSession) {
        log.info("ending " + uconSession);
        EntityManager em = (EntityManager) entityManager.get();
        try {
            uconSession = em.merge(uconSession);
            // first remove possible parent links to avoid constraint
            // problems on session removal.
            Iterator<UconAttribute> i = uconSession.attributes.iterator();
            while(i.hasNext()) {
                UconAttribute a = i.next();
                if(a.parent != null) {
                    a.parent = null;
                    em.merge(a);
                }
                //i.remove();
            }
            //uconSession.attributes.clear();
            em.remove(uconSession);
            em.flush();
            em.clear();
        } catch (Exception e) {
            log.error("while ending session {} with {} attributes: {}", uconSession, uconSession.attributes.size(), e);
            throw e;
        }
        return uconSession;
    }

    @Override
    public Object save(Object o) {
        EntityManager em = (EntityManager) entityManager.get();
        try {
            o = em.merge(o);
        } catch (Exception e) {
            log.error("while saving {}: {}", e.getMessage());
            throw e;
        }
        // warning: returned object returned has transients reset.
        return o;
    }

    @Override
    public UconAttribute newPrivateAttribute(String id, String type, String value, String issuer, UconAttribute parent, String uuid) {
        for (UconAttribute child : parent.getChildren()) {
            if (child.getId().equals(id)) {
                child.setType(type);
                child.setValue(value);
                child.setIssuer(issuer);
                return child;
            }
        }
        PepAttribute a = new PepAttribute(id, type, value, issuer, parent.getCategory(), uuid);
        UconAttribute u = UconAttribute.newInstance(a);
        u = (UconAttribute) save(u);
        // DON'T SAVE ME!
        u.parent = parent;
        parent.children.add(u);
        return u;
    }

    @Override
    public UconAttribute newSharedAttribute(String id, String type, String value, String issuer, String category, String uuid) {
        UconAttribute u = getSharedAttribute(category, id);
        if (u == null) {
            PepAttribute a = new PepAttribute(id, type, value, issuer, category, uuid);
            u = UconAttribute.newInstance(a);
        } else {
            u.setType(type);
            u.setValue(value);
            u.setIssuer(issuer);
        }
        //assert (u.getFactory() != null); FIXME
        //assert(u.getRowId() != null);
        return u;
    }

    @Override
    public UconRequest rebuildUconRequest(UconSession uconSession) {
        log.debug("" + uconSession);
        UconRequest accessRequest = new UconRequest();
        for (UconAttribute a : uconSession.attributes) {
            accessRequest.add(a);
        }
        return accessRequest;
    }

}
