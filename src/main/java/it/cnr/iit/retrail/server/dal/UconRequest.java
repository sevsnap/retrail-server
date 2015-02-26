/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author oneadmin
 */
public class UconRequest extends PepRequest {

    public UconRequest(Document doc) {
        super(doc);
    }

    public UconRequest() {
        super();
    }

    @Override
    public boolean add(PepAttributeInterface a) {
        assert (a instanceof UconAttribute);
        return super.add(a);
    }
/*
    @Override
    public boolean replace(PepAttributeInterface a) {
        assert (a instanceof UconAttribute);
        if(((UconAttribute)a).getRowId() == null)
            add(a);
        //else FIXME
        return true;
    }
  */  
    @Override
    protected PepAttributeInterface newAttribute(Element e) {
        PepAttribute a = new PepAttribute(e);
        return UconAttribute.newInstance(a);
    }

    @Override
    protected PepAttributeInterface newAttribute(String id, String type, String value, String issuer, String category, String factory) {
        PepAttribute tmp = new PepAttribute(id, type, value, issuer, category, factory);
        UconAttribute a = new UconAttribute();
        a.copy(tmp);
        return a;
    }

}
