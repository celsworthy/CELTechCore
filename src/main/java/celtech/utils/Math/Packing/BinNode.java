/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils.Math.Packing;

/**
 *
 * @author Ian
 */
public class BinNode
{
    boolean used = false;
    int x = 0;
    int y = 0;
    int w = 0;
    int h = 0;
    BinNode right;
    BinNode down;

    BinNode(int x, int y, int w, int h)
    {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean isUsed()
    {
        return used;
    }

    public void setUsed(boolean used)
    {
        this.used = used;
    }

    public int getX()
    {
        return x;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public int getY()
    {
        return y;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    public int getW()
    {
        return w;
    }

    public void setW(int w)
    {
        this.w = w;
    }

    public int getH()
    {
        return h;
    }

    public void setH(int h)
    {
        this.h = h;
    }

    public BinNode getRight()
    {
        return right;
    }

    public void setRight(BinNode right)
    {
        this.right = right;
    }

    public BinNode getDown()
    {
        return down;
    }

    public void setDown(BinNode down)
    {
        this.down = down;
    }
    
    
}
