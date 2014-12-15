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

    /**
     *
     */
    ABS("ABS", 1.04),

    /**
     *
     */
    PLA("PLA", 1.25),

    /**
     *
     */
    Nylon("Nylon", 1.134);
    
    private String friendlyName;
     /**
     * Approximate material density in g / cm^3.
     */
    private double density;

    private MaterialType(String friendlyName, Double density)
    {
        this.friendlyName = friendlyName;
        this.density = density;
    }
    
    /**
     *
     * @return
     */
    public String getFriendlyName()
    {
        return friendlyName;
    }
    
    public double getDensity() {
        return density;
    }
    
    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return friendlyName;
    }
}
