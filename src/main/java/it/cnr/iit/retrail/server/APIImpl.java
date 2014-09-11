/* */
package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class APIImpl implements API {
    private static UCon ucon = UCon.getInstance();

    @Override
    public Node tryAccess(Node accessRequest) {
        PepAccessRequest request = new PepAccessRequest((Document) accessRequest);
        PepAccessResponse response =  ucon.tryAccess(request);
        return response.toElement();
    }

    @Override
    public Node startAccess(Node accessRequest, String pepUrl) throws MalformedURLException {
        System.out.println("API.startAccess(pepUrl="+pepUrl+")");
        PepAccessRequest request = new PepAccessRequest((Document) accessRequest);
        PepAccessResponse response =  ucon.startAccess(request, new URL(pepUrl));
        return response.toElement();
    }

    @Override
    public Node endAccess(String sessionId) {
        ucon.endAccess(sessionId);
        return null;
    }

    @Override
    public Node heartbeat(String pepUrl, List<String> sessionsList) throws MalformedURLException, ParserConfigurationException, XmlRpcException, SAXException, IOException {
          System.out.println("API.heartbeat(): called, with url: "+pepUrl);
          return ucon.heartbeat(new URL(pepUrl), sessionsList);
    }
    
    @Override
    public Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        System.out.println("API.echo(): reply service for testing called, with node:");
        DomUtils.write(node);
        return node;
    }

}
