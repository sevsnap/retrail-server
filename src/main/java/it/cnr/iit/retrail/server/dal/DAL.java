/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.Status;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author oneadmin
 */
public class DAL {

    //Create entity manager, this step will connect to database, please check 
    //JDBC driver on classpath, jdbc URL, jdbc driver name on persistence.xml
    private static final String PERSISTENCE_UNIT_NAME = "retrail";
    private static final EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
    protected static final Logger log = LoggerFactory.getLogger(DAL.class);
    final private ThreadLocal entityManager;

    public EntityManager getEntityManager() {
        EntityManager em = (EntityManager) entityManager.get();
        // clearing the entity manager is fundamental to avoid stale objects that
        // may have been updated by other threads!
        em.clear();
        return em;
    }

    private static DAL instance;

    public static DAL getInstance() {
        if (instance == null) {
            instance = new DAL();
        }
        return instance;
    }

    public void debugDump() {
        log.info("current open sessions:");
        Collection<UconSession> q = listSessions();
        for (UconSession s : q) {
            log.info("\t" + s);
            for (Attribute a : s.getAttributes()) {
                log.info("\t\t" + a);
            }
        }
        //System.out.println("DAL: all attributes:");
        //for (Attribute a: listAttributesByFactoryUUID())
        //        System.out.println("\t" + a);
    }

    public void debugDumpAttributes(Collection<Attribute> al) {
        for (Attribute a : al) {
            log.info("\t{} (parent: {})", a, ((Attribute)a.getParent()).getRowId());
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
                return factory.createEntityManager();
            }
        };
        debugDump();
    }

    public Collection<UconSession> listSessions() {
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s",
                UconSession.class);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    public Collection<UconSession> listSessions(Status status) {
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.status = :status",
                UconSession.class)
                .setParameter("status", status);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    public Collection<UconSession> listSessions(Date lastSeenBefore) {
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.lastSeen < :lastSeen",
                UconSession.class)
                .setParameter("lastSeen", lastSeenBefore);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    public Collection<UconSession> listSessions(URL pepUrl) {
        String url = pepUrl.toString();
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.pepUrl = :pepUrl",
                UconSession.class)
                .setParameter("pepUrl", url);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    public Collection<UconSession> listOutdatedSessions() {
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select distinct s from UconSession s, Attribute a where s.status = :status and s member of a.sessions and a.expires < :now",
                UconSession.class)
                .setParameter("status", Status.ONGOING)
                .setParameter("now", new Date());
        Collection<UconSession> involvedSessions = q.getResultList();
        return involvedSessions;
    }

    public Collection<Attribute> listAttributes(URL pepUrl) {
        EntityManager em = getEntityManager();
        TypedQuery<Attribute> q = em.createQuery(
                "select distinct a from Attribute a, UconSession s where s.pepUrl = :pepUrl and s member of a.sessions",
                Attribute.class)
                .setParameter("pepUrl", pepUrl.toString());
        Collection<Attribute> attributes = q.getResultList();
        return attributes;
    }

    public Collection<Attribute> listAttributes() {
        EntityManager em = getEntityManager();
        TypedQuery<Attribute> q = em.createQuery(
                "select a from Attribute a",
                Attribute.class);
        Collection<Attribute> attributes = q.getResultList();
        return attributes;
    }

    public Collection<Attribute> listManagedAttributes(String factory) {
        log.debug("begin");
        EntityManager em = getEntityManager();
        TypedQuery<Attribute> q = em.createQuery(
                "select a from Attribute a where a.factory = :factory and a.expires is null",
                Attribute.class)
                .setParameter("factory", factory);
        Collection<Attribute> attributes = q.getResultList();
        //debugDump();
        log.debug("end");
        return attributes;
    }

    public Collection<Attribute> listUnmanagedAttributes(String factory) {
        log.debug("begin");
        EntityManager em = getEntityManager();
        TypedQuery<Attribute> q = em.createQuery(
                "select a from Attribute a where a.factory = :factory and a.expires is not null",
                Attribute.class)
                .setParameter("factory", factory);
        Collection<Attribute> attributes = q.getResultList();
        //debugDump();
        log.debug("end");
        return attributes;
    }

    private Attribute findParent(EntityManager em, UconSession session, PepAttributeInterface pepAttribute) {

        Attribute parent = null;
        if (pepAttribute.getParent() != null) {
            TypedQuery<Attribute> q = em.createQuery(
                    "select a from Attribute a where a.id = :id and a.category = :category and a.value = :value and :session member of a.sessions",
                    Attribute.class)
                    .setParameter("session", session)
                    .setParameter("id", pepAttribute.getParent().getId())
                    .setParameter("category", pepAttribute.getParent().getCategory())
                    .setParameter("value", pepAttribute.getParent().getValue());
            parent = q.getSingleResult();
        }
        return parent;
    }

    private Attribute updateAttribute(EntityManager em, PepAttributeInterface pepAttribute) {
        Attribute attribute;
        TypedQuery<Attribute> q = em.createQuery(
                "select a from Attribute a where a.id = :id and a.category = :category",// and :session member of a.sessions",
                Attribute.class)
                .setParameter("id", pepAttribute.getId())
                .setParameter("category", pepAttribute.getCategory());
        attribute = q.getSingleResult();
        attribute.copy(pepAttribute, null); // FIXME
        attribute = em.merge(attribute);
        return attribute;
    }

    private Attribute updateAttribute(EntityManager em, PepAttributeInterface pepAttribute, Attribute parent) {
        assert (parent != null);
        Attribute attribute;
        TypedQuery<Attribute> q;
        q = em.createQuery(
                "select a from Attribute a where a.id = :id and a.category = :category and a.parent = :parent",
                Attribute.class)
                .setParameter("id", pepAttribute.getId())
                .setParameter("category", pepAttribute.getCategory())
                .setParameter("parent", parent);
        attribute = q.getSingleResult();
        attribute.copy(pepAttribute, parent);
        attribute = em.merge(attribute);
        return attribute;
    }

    public Collection<UconSession> updateAttributes(Collection<PepAttributeInterface> pepAttributes) {
        EntityManager em = getEntityManager();
        Collection<UconSession> involvedSessions = new HashSet<>();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            for (PepAttributeInterface pepAttribute : pepAttributes) {
                Attribute parent = null;//FIXME findParent(em, pepAttribute);
                Attribute attribute = updateAttribute(em, pepAttribute, parent);
                for (UconSession uconSession : attribute.getSessions()) {
                    if (uconSession.getStatus() == Status.ONGOING) {
                        involvedSessions.add(uconSession);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            log.error("rolling back transaction: {}", e.getMessage());
            em.getTransaction().rollback();
            throw e;
        }
        return involvedSessions;
    }

    private Collection<UconSession> updateSession(EntityManager em, UconSession uconSession, Collection<PepAttributeInterface> pepAttributes) {
        Collection<UconSession> involvedSessions = new HashSet<>();
        LinkedList<PepAttributeInterface> rootsFirst = new LinkedList<>();
        Attribute attribute;
        // put parents in front in order to create them first
        for (PepAttributeInterface pepAttribute : pepAttributes) {
            if (pepAttribute.getParent() == null) {
                rootsFirst.addFirst(pepAttribute);
            } else {
                rootsFirst.addLast(pepAttribute);
            }
        }
        // Then, process all
        for (PepAttributeInterface pepAttribute : rootsFirst) {
            Attribute parent = findParent(em, uconSession, pepAttribute);
            try {
                attribute = updateAttribute(em, pepAttribute);
                // If this attribute is already in the session, remove it first for safety.
                uconSession.removeAttribute(attribute);
            } catch (NoResultException e) {
                attribute = Attribute.newInstance(pepAttribute, parent);
                em.persist(attribute);
            }
            uconSession.addAttribute(attribute);
            involvedSessions.addAll(attribute.getSessions());
        }
        return involvedSessions;
    }

    public UconSession startSession(Collection<PepAttributeInterface> pepAttributes, URL pepUrl, String customId) {
        UconSession uconSession = null;
        // Store request's attributes to the database
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            uconSession = new UconSession();
            uconSession.setPepUrl(pepUrl.toString());
            uconSession.setCustomId(customId);
            uconSession = em.merge(uconSession);
            em.persist(uconSession);
            uconSession = em.merge(uconSession);
            if (customId == null || customId.length() == 0) {
                uconSession.setCustomId(uconSession.getUuid());
                uconSession = em.merge(uconSession);
            }
            LinkedList<PepAttributeInterface> rootsFirst = new LinkedList<>();
            Map<PepAttributeInterface, Attribute> map = new HashMap<>();
            // put parents in front in order to create them first
            for (PepAttributeInterface pepAttribute : pepAttributes) {
                if (pepAttribute.getParent() == null) {
                    rootsFirst.addFirst(pepAttribute);
                } else {
                    rootsFirst.addLast(pepAttribute);
                }
            }
            // Then, process all
            for (PepAttributeInterface pepAttribute : rootsFirst) {
                Attribute parent = map.getOrDefault(pepAttribute.getParent(), null);
                Attribute attribute = Attribute.newInstance(pepAttribute, parent);
                em.persist(attribute);
                uconSession.addAttribute(attribute);
                map.put(pepAttribute, attribute);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        }
        return uconSession;
    }

    @Deprecated
    public void updateSession(UconSession uconSession, Collection<PepAttributeInterface> pepAttributes) {
        // Store request's attributes to the database
        log.debug("begin " + uconSession);
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        uconSession = em.merge(uconSession);

        try {
            updateSession(em, uconSession, pepAttributes);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        }
        log.debug("end " + uconSession);

    }

    public UconSession getSession(String uuid) {
        // TODO use custom generated value
        EntityManager em = getEntityManager();
        return em.find(UconSession.class, uuid);
    }

    @Deprecated
    public UconSession getSessionByCustomId(String customId) {
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.customId = :id",
                UconSession.class)
                .setParameter("id", customId);
        UconSession uconSession = q.getSingleResult();
        return uconSession;
    }

    private void removeAttributes(EntityManager em, UconSession uconSession) {
        log.debug("removing all attributes for " + uconSession);
        while (uconSession.getAttributes().size() > 0) {
            Attribute a = uconSession.getAttributes().iterator().next();
            a.setParent(null);
            uconSession.removeAttribute(a);
            if (a.getSessions().isEmpty()) {
                em.remove(a);
            }
        }
        uconSession.getAttributes().clear();
    }

    public UconSession revokeSession(UconSession uconSession) {
        log.info("revoking " + uconSession);
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            uconSession = em.merge(uconSession);
            removeAttributes(em, uconSession);
            uconSession.setStatus(Status.REVOKED);
            uconSession = em.merge(uconSession);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        }
        //debugDump();
        return uconSession;
    }

    public UconSession endSession(UconSession uconSession) {
        log.info("ending " + uconSession);
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            uconSession = em.merge(uconSession);
            if (uconSession != null) {
                removeAttributes(em, uconSession);
                uconSession = em.merge(uconSession);
                em.remove(uconSession);
            } else {
                log.error("cannot find {}", uconSession);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            log.error("unexpected exception when ending {}: {}", uconSession, e);
            em.getTransaction().rollback();
            throw e;
        }
        //debugDump();
        return uconSession;
    }

    public Object save(Object o) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        try {
            o = em.merge(o);
            em.getTransaction().commit();
        } catch (Exception e) {
            log.error(e.getMessage());
            em.getTransaction().rollback();
            throw e;
        }
        return o;
    }
}
