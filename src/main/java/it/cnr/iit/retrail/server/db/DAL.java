/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.server.db;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.commons.PepSession;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
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
        return (EntityManager) entityManager.get();
    }

    private static DAL instance;

    public static DAL getInstance() {
        if (instance == null) {
            instance = new DAL();
        }
        return instance;
    }

    private void debugDump() {
        EntityManager em = getEntityManager();
        log.info("current open sessions:");
        TypedQuery<UconSession> q = em.createQuery("select session from UconSession session", UconSession.class);
        for (UconSession s : q.getResultList()) {
            log.info("\t" + s);
            for (Attribute a : s.getAttributes()) {
                log.info("\t\t" + a);
            }
        }
        //System.out.println("DAL: all attributes:");
        //for (Attribute a: listAttributes())
        //        System.out.println("\t" + a);
    }

    private DAL() {
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

    public Collection<Attribute> listAttributes(Date invalidAfter) {
        EntityManager em = getEntityManager();
        TypedQuery<Attribute> q = em.createQuery(
                "select a from Attribute a where a is not null and a.expires < :invalidAfter",
                Attribute.class)
                .setParameter("invalidAfter", invalidAfter);
        Collection<Attribute> attributes = q.getResultList();
        return attributes;
    }

    public Collection<Attribute> listAttributes(URL pepUrl) {
        String url = pepUrl.toString();
        EntityManager em = getEntityManager();
        TypedQuery<Attribute> q = em.createQuery(
                "select distinct a from Attribute a, UconSession s where s.pepUrl = :pepUrl and s member of a.sessions",
                Attribute.class)
                .setParameter("pepUrl", pepUrl);
        Collection<Attribute> attributes = q.getResultList();
        return attributes;
    }

    private void updateSession(EntityManager em, UconSession uconSession, Collection<PepRequestAttribute> pepAttributes) {
        for (PepRequestAttribute pepAttribute : pepAttributes) {
            Attribute attribute;
            TypedQuery<Attribute> q = em.createQuery(
                    "select a from Attribute a where a.id = :id and a.category = :category",
                    Attribute.class)
                    .setParameter("id", pepAttribute.id)
                    .setParameter("category", pepAttribute.category);
            try {
                attribute = q.getSingleResult();
                attribute.copy(pepAttribute);
                attribute = em.merge(attribute);
                // If this attribute is already in the session, remove it first for safety.
                uconSession.removeAttribute(attribute);
            } catch (NoResultException e) {
                attribute = Attribute.newInstance(pepAttribute);
                em.persist(attribute);
            }
            uconSession.addAttribute(attribute);
        }
    }

    public UconSession startSession(Collection<PepRequestAttribute> pepAttributes, URL pepUrl) {
        UconSession uconSession = null;
        // Store request's attributes to the database
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            uconSession = new UconSession();
            uconSession.setPepUrl(pepUrl.toString());
            em.persist(uconSession);
            updateSession(em, uconSession, pepAttributes);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        }
        return uconSession;
    }

    public void updateSession(UconSession uconSession, Collection<PepRequestAttribute> pepAttributes) {
        // Store request's attributes to the database
        log.info("" + uconSession);
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            updateSession(em, uconSession, pepAttributes);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        }
        debugDump();
    }

    public UconSession getSession(Long sessionId) {
        EntityManager em = getEntityManager();
        UconSession uconSession = em.find(UconSession.class, sessionId);
        return uconSession;
    }

    private void removeAttributes(EntityManager em, UconSession uconSession) {
        log.info("removing all attributes for " + uconSession);
        while (uconSession.getAttributes().size() > 0) {
            Attribute a = uconSession.getAttributes().iterator().next();
            uconSession.removeAttribute(a);
            if (a.getSessions().isEmpty()) {
                em.remove(a);
            }
        }
        uconSession.getAttributes().clear();
    }

    public UconSession revokeSession(Long sessionId) {
        log.info("revoking " + sessionId);
        UconSession uconSession = null;
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            uconSession = em.find(UconSession.class, sessionId);
            if (uconSession != null) {
                removeAttributes(em, uconSession);
                uconSession.setStatus(PepSession.Status.REVOKED);
                uconSession = em.merge(uconSession);
            } else {
                log.error("cannot find session with id={}", sessionId);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        }
        debugDump();
        return uconSession;
    }

    public UconSession endSession(Long sessionId) {
        log.info("removing " + sessionId);
        UconSession uconSession = null;
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            uconSession = em.find(UconSession.class, sessionId);
            if (uconSession != null) {
                removeAttributes(em, uconSession);
                uconSession = em.merge(uconSession);
                em.remove(uconSession);
            } else {
                log.error("cannot find session with id={}", sessionId);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        }
        debugDump();
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
