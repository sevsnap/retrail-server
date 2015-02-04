/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.UConProtocol;
import java.util.List;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Node;

public class UConProtocolProxy implements UConProtocol {

    @Override
    public final Node tryAccess(Node accessRequest, String pepUrl, String customId) throws Exception {
        return UCon.getProtocolInstance().tryAccess(accessRequest, pepUrl, customId);
    }

    @Override
    public final Node startAccess(String uuid, String customId) throws Exception {
        return UCon.getProtocolInstance().startAccess(uuid, customId);
    }

    @Override
    public final List<Node> endAccess(List<String> uuidList, List<String> customIdList) throws Exception {
        return UCon.getProtocolInstance().endAccess(uuidList, customIdList);
    }

    @Override
    public final Node heartbeat(String pepUrl, List<String> sessionsList) throws Exception {
        return UCon.getProtocolInstance().heartbeat(pepUrl, sessionsList);
    }

    @Override
    public final Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        return UCon.getProtocolInstance().echo(node);
    }

    @Override
    public final Node assignCustomId(String uuid, String oldCustomId, String newCustomId) throws Exception {
        return UCon.getProtocolInstance().assignCustomId(uuid, oldCustomId, newCustomId);
    }

}
