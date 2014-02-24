/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils.Math;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 *
 */
public class PolarCoordinate
{

    private double theta; //Elevation in radians
    private double phi; //Azimuth in radians
    private double radius;

    public PolarCoordinate(double theta, double phi, double radius)
    {
        this.theta = theta;
        this.phi = phi;
        this.radius = radius;
    }

    public double getTheta()
    {
        return theta;
    }

    public void setTheta(double theta)
    {
        this.theta = theta;
    }

    public void setThetaDegrees(double thetaInDegrees)
    {
        this.theta = Math.toRadians(thetaInDegrees);
    }

    public double getPhi()
    {
        return phi;
    }

    public void setPhi(double phi)
    {
        this.phi = phi;
    }

    public void setPhiDegrees(double phiInDegrees)
    {
        this.phi = Math.toRadians(phiInDegrees);
    }

    public double getRadius()
    {
        return radius;
    }

    public void setRadius(double radius)
    {
        this.radius = radius;
    }

    public String toString()
    {
        return "Theta:" + getTheta() + " Phi:" + getPhi() + " Radius:" + getRadius();
    }

}
