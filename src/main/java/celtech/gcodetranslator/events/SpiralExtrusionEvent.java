/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class SpiralExtrusionEvent extends GCodeParseEvent
{
    private float z;
    private float x;
    private float y;
    private float e;

    /**
     *
     * @return
     */
    public float getZ()
    {
        return z;
    }

    /**
     *
     * @param z
     */
    public void setZ(float z)
    {
        this.z = z;
    }

    /**
     *
     * @return
     */
    public float getX()
    {
        return x;
    }

    /**
     *
     * @param x
     */
    public void setX(float x)
    {
        this.x = x;
    }

    /**
     *
     * @return
     */
    public float getY()
    {
        return y;
    }

    /**
     *
     * @param y
     */
    public void setY(float y)
    {
        this.y = y;
    }

    /**
     *
     * @return
     */
    public float getE()
    {
        return e;
    }

    /**
     *
     * @param e
     */
    public void setE(float e)
    {
        this.e = e;
    }

    /**
     *
     * @return
     */
    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 X" + String.format("%.3f", x) + " Y" + String.format("%.3f", y) + " E" + String.format("%.5f", e);
        
        if (getFeedRate() > 0)
        {
            stringToReturn += " F" + String.format("%.3f", getFeedRate());
        }
        
        if (getComment() != null)
        {
            stringToReturn += " ; " +  getComment();
        }
        
        return stringToReturn + "\n";
    }
}
