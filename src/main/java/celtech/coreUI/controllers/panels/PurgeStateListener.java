/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.coreUI.controllers.panels;

import celtech.services.purge.PurgeState;

/**
 *
 * @author Ian
 */
public interface PurgeStateListener
{
    public void setState(PurgeState state);
}
