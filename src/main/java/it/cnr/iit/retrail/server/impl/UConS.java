/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.commons.DomUtils;
import java.net.URL;
import org.opensaml.Configuration;

import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.impl.SignatureBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
public class UConS extends UCon {
    private static final AssertionBuilder assertionBuilder = new AssertionBuilder();
    private static final SignatureBuilder signatureBuilder = new SignatureBuilder();
    
    protected UConS(URL url) throws Exception {
        super(url);
        assert(assertionBuilder != null);
        assert(signatureBuilder != null);
    }

    private Node sign(Element doc) throws SignatureException, MarshallingException {
        if(false) {
        log.info("signing response with opensaml, {}", DomUtils.toString(doc));
        Assertion assertion = assertionBuilder.buildObject(doc);
        //Credential signingCredential = getSigningCredential();

        Signature signature = signatureBuilder.buildObject(Signature.DEFAULT_ELEMENT_NAME);
        
        //signature.setSigningCredential(signingCredential);
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        //signature.setKeyInfo(getKeyInfo(signingCredential));
 
        assertion.setSignature(signature);
        
        Configuration.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
        Signer.signObject(signature);

        Node node = assertion.getDOM();
        log.warn("signed document is: {}", DomUtils.toString(node));
        return node;
        } else
        return doc;
    }

    @Override
    public Node endAccess(String uuid, String customId) throws Exception {
        Element doc = (Element) super.endAccess(uuid, customId); //To change body of generated methods, choose Tools | Templates.
        return sign(doc);
    }

    @Override
    public Node startAccess(String uuid, String customId) throws Exception {
        Element doc = (Element) super.startAccess(uuid, customId); //To change body of generated methods, choose Tools | Templates.
        return sign(doc);
    }

    @Override
    public Node assignCustomId(String uuid, String oldCustomId, String newCustomId) throws Exception {
        Element doc = (Element) super.assignCustomId(uuid, oldCustomId, newCustomId); //To change body of generated methods, choose Tools | Templates.
        return sign(doc);
    }

    @Override
    public Node tryAccess(Node accessRequest, String pepUrlString, String customId) throws Exception {
        Element doc = (Element) super.tryAccess(accessRequest, pepUrlString, customId);
        return sign(doc);
    }

}
