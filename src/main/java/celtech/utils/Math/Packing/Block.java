/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils.Math.Packing;

import celtech.modelcontrol.ModelContainer;

/**
 *
 * @author Ian
 */
public class Block
{

    private ModelContainer model = null;
    private int halfPadding = 0;
    int w = 0;
    int h = 0;
    BinNode fit = null;

    /**
     *
     * @param model
     * @param padding
     */
    public Block(ModelContainer model, int padding)
    {
        this.model = model;
        this.halfPadding = padding / 2;
        this.w = (int) model.getTotalWidth() + padding;
        this.h = (int) model.getTotalDepth() + padding;
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
            model.translateFrontLeftTo((double) fit.getX() + halfPadding, (double) fit.getY() + halfPadding);
        }
    }

}
