/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.server.db;

import it.cnr.iit.retrail.commons.PepRequestAttribute;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

/**
 *
 * @author oneadmin
 */
public class DAL {

    private static final String PERSISTENCE_UNIT_NAME = "retrail";
    private final EntityManagerFactory factory;

    private static DAL instance;

    public static DAL getInstance() {
        if (instance == null) {
            instance = new DAL();
        }
        return instance;
    }

    private void debugDump() {
        EntityManager em = factory.createEntityManager();
        System.out.println("DAL: current open sessions:");
        TypedQuery<UconSession> q = em.createQuery("select session from UconSession session", UconSession.class);
        for (UconSession s : q.getResultList()) {
            System.out.println("DAL: " + s);
            for (Attribute a : s.getAttributes()) {
                System.out.println("\t" + a);
            }
        }
        //System.out.println("DAL: all attributes:");
        //for (Attribute a: listAttributes())
        //        System.out.println("\t" + a);
        em.close();
    }

    private DAL() {
        //Create entity manager, this step will connect to database, please check 
        //JDBC driver on classpath, jdbc URL, jdbc driver name on persistence.xml
        factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        debugDump();
    }

    public Collection<UconSession> listSessions() {
        EntityManager em = factory.createEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s",
                UconSession.class);
        Collection<UconSession> sessions = q.getResultList();
        em.close();
        return sessions;
    }

    public Collection<Attribute> listAttributes() {
        EntityManager em = factory.createEntityManager();
        TypedQuery<Attribute> q = em.createQuery(
                "select a from Attribute a",
                Attribute.class);
        Collection<Attribute> attributes = q.getResultList();
        em.close();
        return attributes;
    }

    public UconSession startSession(List<PepRequestAttribute> pepAttributes) {
        // Store request's attributes to the database
        EntityManager em = factory.createEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        UconSession uconSession = new UconSession();
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
            } catch (NoResultException e) {
                attribute = Attribute.newInstance(pepAttribute);
                em.persist(attribute);
            }
            uconSession.addAttribute(attribute);
        }
        em.getTransaction().commit();
        em.close();
        debugDump();
        return uconSession;
    }

    public void endSession(Long sessionId) {
        EntityManager em = factory.createEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        UconSession uconSession = em.find(UconSession.class, sessionId);
        if(uconSession != null) {
            for(Attribute a: uconSession.getAttributes()) {
                a.getSessions().remove(uconSession);
                a = em.merge(a);
                if(a.getSessions().isEmpty())
                    em.remove(a);
            }
            uconSession.getAttributes().clear();
            uconSession = em.merge(uconSession);
            em.remove(uconSession);
        }
        em.getTransaction().commit();
        em.close();
        debugDump();
    }
    
}
