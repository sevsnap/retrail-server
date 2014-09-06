/* */
package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class API {

    public Node tryAccess(Node accessRequest) {
        PepAccessRequest request = new PepAccessRequest((Document) accessRequest);
        return UCon.getInstance().tryAccess(request);
    }

    public Node startAccess(Node accessRequest) {
        PepAccessRequest request = new PepAccessRequest((Document) accessRequest);
        return UCon.getInstance().startAccess(request);
    }

    public Node endAccess(Node accessRequest) {
        PepAccessRequest request = new PepAccessRequest((Document) accessRequest);
        return UCon.getInstance().endAccess(request);
    }

    public Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        System.out.println("API.echo(): reply service for testing called, with node:");
        DomUtils.write(node);
        return node;
    }

}
