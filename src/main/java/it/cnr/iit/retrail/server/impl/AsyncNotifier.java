/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.commons.RecorderInterface;
import it.cnr.iit.retrail.commons.impl.Client;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.pip.SystemEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.Element;

/**
 *
 * @author oneadmin
 */
public class AsyncNotifier extends Client implements RecorderInterface, Runnable {
    private Thread thread;
    final private UCon ucon;
    private File recorderFile = null;
    private boolean mustAppendToRecorderFile = false;
    private long recorderMillis = 0;

    private class Operation {

        String operation;
        URL pepUrl;
        UconSession session;

        Operation(String o, URL u, UconSession s) {
            operation = o;
            pepUrl = u;
            session = s;
        }
    }

    private final LinkedList<Operation> queue;

    public AsyncNotifier(UCon ucon) throws Exception {
        super(null);
        this.ucon = ucon;
        queue = new LinkedList<>();
    }

    public void init() {
        thread = new Thread(this, getClass().getSimpleName());
        thread.start();
    }
    
    public void term() throws InterruptedException {
        thread.interrupt();
        thread.join();
    }
    
    @Override
    public void run() {
        log.info("starting asynchronous notification service");
        boolean interrupted = false;
        Collection<UconSession> sessions = new ArrayList<>(64);
        while (!interrupted) {
            Operation op = null;
            synchronized (queue) {
                try {
                    while (queue.isEmpty()) {
                        queue.wait();
                    }
                    op = queue.removeFirst();
                    sessions.clear();
                    sessions.add(op.session);
                    // Collect sessions for the same url and operation
                    for (Iterator<Operation> i = queue.iterator(); i.hasNext();) {
                        Operation o = i.next();
                        if (o.pepUrl.equals(op.pepUrl) && o.operation.equals(op.operation)) {
                            sessions.add(o.session);
                            i.remove();
                        }
                    }
                    // Now perform operation
                    if(sessions.size() > 1)
                        log.warn("performing bulk {} for {} sessions", op.operation, sessions.size());
                    ucon.getDAL().begin();
                    try {
                        switch (op.operation) {
                            case "PEP.revokeAccess":
                                _revokeAccess(op.pepUrl, sessions);
                                break;
                            case "PEP.runObligations":
                                _runObligations(op.pepUrl, sessions);
                                break;
                            default:
                                log.error("unknown operation: {}", op.operation);
                        }
                        ucon.getDAL().commit();
                    } catch (Exception e) {
                        ucon.getDAL().rollback();
                        throw e;
                    }
                } catch (InterruptedException ex) {
                    interrupted = true;
                    break;
                } catch (Exception e) {
                    log.error("while executing operation {}: {}", op, e);
                }
            }
        }
        log.info("asynchronous notification service terminated");
    }

    private Object[] rpc(URL pepUrl, String api, List<Element> responses) throws Exception {
        // create client
        log.info("invoking {} at {}", api, pepUrl);
        config.setServerURL(pepUrl);
        setConfig(config);
        if (recorderFile != null) {
            if (mustAppendToRecorderFile) {
                continueRecording(recorderFile, recorderMillis);
            } else {
                startRecording(recorderFile);
                mustAppendToRecorderFile = true;
            }
        }
        // remote call. TODO: should consider error handling
        Object[] params = new Object[]{responses};
        Object[] rv = (Object[]) execute(api, params);
        if (recorderFile != null) {
            recorderMillis = getMillis();
            stopRecording();
        }
        return rv;
    }

    private List<Element> _revokeAccess(URL pepUrl, Collection<UconSession> sessions) throws Exception {
        List<Element> responses = new ArrayList<>(sessions.size());

        for (UconSession uconSession : sessions) {
            long start = System.currentTimeMillis();
            // XXX Rebuilding the request may be too heavy. 
            // We should provide a way to dynamically get it if the PIP needs it.
            ucon.getPIPChain().fireSystemEvent(new SystemEvent(SystemEvent.EventType.beforeRevokeAccess, null, uconSession));
            uconSession.setMs(System.currentTimeMillis() - start);
            Element sessionElement = uconSession.toXacml3Element();
            responses.add(sessionElement);
        }

        // TODO: check error
        Object ack = rpc(pepUrl, "PEP.revokeAccess", responses);

        for (UconSession uconSession : sessions) {
            ucon.getPIPChain().fireSystemEvent(new SystemEvent(SystemEvent.EventType.afterRevokeAccess, null, uconSession, ack));
            ucon.getDAL().save(uconSession);
        }
        return responses;
    }

    private void _runObligations(URL pepUrl, Collection<UconSession> uconSessions) throws Exception {
        List<Element> responses = new ArrayList<>(uconSessions.size());
        Collection<UconSession> uconSessions2 = new ArrayList<>(uconSessions.size());
        for (UconSession uconSession : uconSessions) {
            // revoke session on db
            UconRequest uconRequest = ucon.getDAL().rebuildUconRequest(uconSession);
            ucon.getPIPChain().refresh(uconRequest, uconSession);
            ucon.getPIPChain().fireSystemEvent(new SystemEvent(SystemEvent.EventType.beforeRunObligations, uconRequest, uconSession));
            Element sessionElement = uconSession.toXacml3Element();
            responses.add(sessionElement);
            // save ucon session
            uconSession = (UconSession) ucon.getDAL().save(uconSession);
            uconSessions2.add(uconSession);
        }
        // TODO: check error
        // TODO: returned docs are currently ignored. 
        // should use them for some back ack
        Object ack = rpc(pepUrl, "PEP.runObligations", responses);
        for (UconSession uconSession : uconSessions2) {
            UconRequest uconRequest = ucon.getDAL().rebuildUconRequest(uconSession);
            ucon.getPIPChain().fireSystemEvent(new SystemEvent(SystemEvent.EventType.afterRunObligations, uconRequest, uconSession, ack));
            uconSession = (UconSession) ucon.getDAL().save(uconSession);
        }
    }

    public void revokeAccess(URL pepUrl, UconSession session) throws Exception {
        Operation operation = new Operation("PEP.revokeAccess", pepUrl, session);
        synchronized (queue) {
            queue.add(operation);
            queue.notifyAll();
        }
    }

    public void runObligations(URL pepUrl, UconSession session) throws Exception {
        Operation operation = new Operation("PEP.runObligations", pepUrl, session);
        synchronized (queue) {
            queue.add(operation);
            queue.notifyAll();
        }
    }

    @Override
    public void startRecording(File outputFile) throws Exception {
        recorderFile = outputFile;
        mustAppendToRecorderFile = false;
        recorderMillis = 0;
    }

    @Override
    public void continueRecording(File outputFile, long millis) throws Exception {
        recorderFile = outputFile;
        mustAppendToRecorderFile = true;
        recorderMillis = millis;
    }

    @Override
    public boolean isRecording() {
        return recorderFile != null;
    }

    @Override
    public void stopRecording() {
        recorderFile = null;
        mustAppendToRecorderFile = false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
