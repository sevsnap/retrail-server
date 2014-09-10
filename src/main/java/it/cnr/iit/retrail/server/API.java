/* */
package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class API {

    public Node tryAccess(Node accessRequest) {
        PepAccessRequest request = new PepAccessRequest((Document) accessRequest);
        PepAccessResponse response =  UCon.getInstance().tryAccess(request);
        return response.toElement();
    }

    public Node startAccess(Node accessRequest, String pepUrl) throws MalformedURLException {
        System.out.println("API.startAccess(): pepUrl:"+pepUrl);
        PepAccessRequest request = new PepAccessRequest((Document) accessRequest);
        PepAccessResponse response =  UCon.getInstance().startAccess(request, new URL(pepUrl));
        return response.toElement();
    }

    public Node endAccess(String sessionId) {
        UCon.getInstance().endAccess(sessionId);
        return null;
    }

    public Node heartbeat(String pepUrl) throws MalformedURLException, ParserConfigurationException, XmlRpcException {
          System.out.println("API.heartbeat(): called, with url: "+pepUrl);
          return UCon.getInstance().heartbeat(new URL(pepUrl));
    }
    
    public Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        System.out.println("API.echo(): reply service for testing called, with node:");
        DomUtils.write(node);
        return node;
    }

}
