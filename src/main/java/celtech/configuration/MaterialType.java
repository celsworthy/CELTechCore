/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.configuration;

/**
 *
 * @author Ian
 */
public enum MaterialType
{
    ABS("ABS"), PLA("PLA"), Nylon("Nylon");
    
    private String friendlyName;

    private MaterialType(String friendlyName)
    {
        this.friendlyName = friendlyName;
    }
    
    public String getFriendlyName()
    {
        return friendlyName;
    }
    
    @Override
    public String toString()
    {
        return friendlyName;
    }
}
