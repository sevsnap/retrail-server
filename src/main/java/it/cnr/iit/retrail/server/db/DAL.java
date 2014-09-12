/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.server.db;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
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

    public Collection<Attribute> listAttributes() {
        EntityManager em = getEntityManager();
        TypedQuery<Attribute> q = em.createQuery(
                "select a from UconSession s join Attribute a on where s.pepUrl = :pepUrl",
                Attribute.class);
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

    public UconSession startSession(List<PepRequestAttribute> pepAttributes, URL pepUrl) {
        UconSession uconSession = null;
        // Store request's attributes to the database
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            uconSession = new UconSession();
            uconSession.setPepUrl(pepUrl.toString());
            em.persist(uconSession);
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
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        }
        debugDump();
        return uconSession;
    }

    public UconSession getSession(Long sessionId) {
        EntityManager em = getEntityManager();
        UconSession uconSession = em.find(UconSession.class, sessionId);
        return uconSession;
    }

    public UconSession endSession(Long sessionId) {
        UconSession uconSession = null;
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
        uconSession = em.find(UconSession.class, sessionId);
        if (uconSession != null) {
            for (Attribute a : uconSession.getAttributes()) {
                a.getSessions().remove(uconSession);
                a = em.merge(a);
                if (a.getSessions().isEmpty()) {
                    em.remove(a);
                }
            }
            uconSession.getAttributes().clear();
            uconSession = em.merge(uconSession);
            em.remove(uconSession);
        }
        em.getTransaction().commit();
        } catch(Exception e) {
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
        }
        catch(Exception e) {
            log.error("*** EXCEPTION: "+e.getMessage());
            em.getTransaction().rollback();
            throw e;
        }
        return o;
    }
}
