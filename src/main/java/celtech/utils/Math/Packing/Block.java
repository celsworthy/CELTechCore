package celtech.utils.Math.Packing;

import celtech.coreUI.visualisation.metaparts.Part;
import celtech.modelcontrol.ModelContainer;

/**
 *
 * @author Ian
 */
public class Block
{

    private Part part = null;
    private int halfPadding = 0;
    int w = 0;
    int h = 0;
    BinNode fit = null;

    /**
     *
     * @param part
     * @param padding
     */
    public Block(Part part, int padding)
    {
        this.part = part;
        this.halfPadding = padding / 2;
        this.w = (int) part.getTotalWidth() + padding;
        this.h = (int) part.getTotalDepth() + padding;
    }

    /**
     *
     * @return
     */
    public int getW()
    {
        return w;
    }

    /**
     *
     * @param w
     */
    public void setW(int w)
    {
        this.w = w;
    }

    /**
     *
     * @return
     */
    public int getH()
    {
        return h;
    }

    /**
     *
     * @param h
     */
    public void setH(int h)
    {
        this.h = h;
    }

    /**
     *
     * @return
     */
    public BinNode getFit()
    {
        return fit;
    }

    /**
     *
     * @param fit
     */
    public void setFit(BinNode fit)
    {
        this.fit = fit;
    }

    /**
     *
     */
    public void relocate()
    {
        if (fit != null)
        {
            part.translateFrontLeftTo((double) fit.getX() + halfPadding, (double) fit.getY() + halfPadding);
        }
    }

}
