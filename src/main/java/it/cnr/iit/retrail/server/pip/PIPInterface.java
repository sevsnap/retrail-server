/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server.pip;

import it.cnr.iit.retrail.commons.PepAccessRequest;

/**
 *
 * @author oneadmin
 */
public interface PIPInterface {

    String getUUID();

    void init();

    void onTryAccess(PepAccessRequest request);

    void onStartAccess(PepAccessRequest request);

    void onRevokeAccess(PepAccessRequest request);

    void onEndAccess(PepAccessRequest request);

    void refresh(PepAccessRequest accessRequest);

    void term();
    
}
