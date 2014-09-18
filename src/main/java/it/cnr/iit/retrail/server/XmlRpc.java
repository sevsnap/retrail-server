/* */
package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XmlRpc implements XmlRpcInterface {

    protected static final Logger log = LoggerFactory.getLogger(XmlRpc.class);
    private static final UCon ucon = UCon.getInstance();

    @Override
    public Node tryAccess(Node accessRequest, String pepUrl, String customId) throws MalformedURLException {
        log.info("pepUrl={}, customId={}", pepUrl, customId);
        PepAccessRequest request = new PepAccessRequest((Document) accessRequest);
        PepAccessResponse response = ucon.tryAccess(request, new URL(pepUrl), customId);
        return response.toElement();
    }

    @Override
    public Node startAccess(String uuid, String customId) throws MalformedURLException {
        log.info("uuid={}, customId={}", uuid, customId);
        uuid = ucon.getUuid(uuid, customId);
        PepAccessResponse response = ucon.startAccess(uuid);
        return response.toElement();
    }

    @Override
    public Node endAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        uuid = ucon.getUuid(uuid, customId);
        return ucon.endAccess(uuid);
    }

    @Override
    public Node heartbeat(String pepUrl, List<String> sessionsList) throws Exception {
        log.debug("called, with url: " + pepUrl);
        return ucon.heartbeat(new URL(pepUrl), sessionsList);
    }

    @Override
    public Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        log.info("reply service for testing called, with node: {}", node);
        return node;
    }

    @Override
    public Node assignCustomId(String uuid, String oldCustomId, String newCustomId) {
        log.info("uuid={}, oldCustomId={}, newCustomId={}", uuid, oldCustomId, newCustomId);
        uuid = ucon.getUuid(uuid, oldCustomId);
        return ucon.assignCustomId(uuid, newCustomId);
    }

}
