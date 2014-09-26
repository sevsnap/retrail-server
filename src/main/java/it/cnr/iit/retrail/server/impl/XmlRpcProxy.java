/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.XmlRpcInterface;
import java.util.List;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Node;

public class XmlRpcProxy implements XmlRpcInterface {
    private static final XmlRpcInterface xmlRpc = UCon.getXmlRpcInstance();

    @Override
    public Node tryAccess(Node accessRequest, String pepUrl, String customId) throws Exception {
        return xmlRpc.tryAccess(accessRequest, pepUrl, customId);
    }

    @Override
    public Node startAccess(String uuid, String customId) throws Exception {
        return xmlRpc.startAccess(uuid, customId);
    }

    @Override
    public Node endAccess(String uuid, String customId) throws Exception {
        return xmlRpc.endAccess(uuid, customId);
    }

    @Override
    public Node heartbeat(String pepUrl, List<String> sessionsList) throws Exception {
        return xmlRpc.heartbeat(pepUrl, sessionsList);
    }

    @Override
    public Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        return xmlRpc.echo(node);
    }

    @Override
    public Node assignCustomId(String uuid, String oldCustomId, String newCustomId) throws Exception {
        return xmlRpc.assignCustomId(uuid, oldCustomId, newCustomId);
    }

}
