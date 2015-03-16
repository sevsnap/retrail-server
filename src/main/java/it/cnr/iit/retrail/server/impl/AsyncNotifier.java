/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.commons.RecorderInterface;
import it.cnr.iit.retrail.commons.impl.Client;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
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
        Element xacmlResponse;

        Operation(String o, URL u, Element xacmlResponse) {
            operation = o;
            pepUrl = u;
            this.xacmlResponse = xacmlResponse;
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
        List<Element> sessions = new ArrayList<>(64);
        Operation op = null;
        while (true) {
            try {
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        queue.wait();
                    }
                    op = queue.removeFirst();
                    sessions.clear();
                    sessions.add(op.xacmlResponse);
                    // Collect sessions for the same url and operation
                    for (Iterator<Operation> i = queue.iterator(); i.hasNext();) {
                        Operation o = i.next();
                        if (o.pepUrl.equals(op.pepUrl) && o.operation.equals(op.operation)) {
                            sessions.add(o.xacmlResponse);
                            i.remove();
                        }
                    }
                }
                // Now perform operation
                rpc(op.pepUrl, op.operation, sessions);
                op = null;
            } catch (InterruptedException ex) {
                log.debug("interrupted: {}", ex);
                break;
            } catch (Exception e) {
                log.error("while executing operation {}: {}", op, e);
            }
        }
        log.info("asynchronous notification service terminated");
    }

    private Object[] rpc(URL pepUrl, String api, List<Element> responses) throws Exception {
        // create client
        if (responses.size() > 1) {
            log.warn("performing bulk {} for {} responses", api, responses.size());
        } else 
            log.info("invoking {} at {}", api, pepUrl);
        config.setServerURL(pepUrl);
        setConfig(config);
        if (recorderFile != null) {
            if (mustAppendToRecorderFile) {
                super.continueRecording(recorderFile, recorderMillis);
            } else {
                super.startRecording(recorderFile);
                mustAppendToRecorderFile = true;
            }
        } 
        // remote call. TODO: should consider error handling
        Object[] params = new Object[]{responses};
        Object[] rv = (Object[]) execute(api, params);
        if (recorderFile != null) {
            recorderMillis = getMillis();
            super.stopRecording();
        }
        return rv;
    }

    public void revokeAccess(URL pepUrl, Element xacmlResponse) {
        Operation operation = new Operation("PEP.revokeAccess", pepUrl, xacmlResponse);
        synchronized (queue) {
            queue.add(operation);
            queue.notifyAll();
        }
    }

    public void runObligations(URL pepUrl, Element xacmlResponse) {
        Operation operation = new Operation("PEP.runObligations", pepUrl, xacmlResponse);
        synchronized (queue) {
            queue.add(operation);
            queue.notifyAll();
        }
    }

    @Override
    public void startRecording(File outputFile) throws Exception {
        assert(outputFile != null);
        recorderFile = outputFile;
        mustAppendToRecorderFile = false;
        recorderMillis = 0;
    }

    @Override
    public void continueRecording(File outputFile, long millis) throws Exception {
        assert(outputFile != null);
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
