package celtech.utils.threed.importers.svg;

import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.importers.twod.svg.metadata.SVGMetaPart;
import java.io.Serializable;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 *
 * @author ianhudson
 */
public class RenderableSVG extends ProjectifiableThing implements Serializable
{
    private static final long serialVersionUID = 1L;

    //Keep these parts for printing later on
    private List<SVGMetaPart> metaparts;

    public RenderableSVG()
    {
    }

    public void setMetaparts(List<SVGMetaPart> metaparts)
    {
        this.metaparts = metaparts;
    }
    
    public List<SVGMetaPart> getMetaparts()
    {
        return metaparts;
    }

    @Override
    public State getState()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setState(State state)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getXScale()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setXScale(double scaleFactor)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getYScale()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setYScale(double scaleFactor)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getZScale()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setZScale(double scaleFactor)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ProjectifiableThing makeCopy()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearElements()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addChildNodes(ObservableList<Node> nodes)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addChildNode(Node node)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservableList<Node> getChildNodes()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void translateBy(double xMove, double zMove)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void translateBy(double xMove, double yMove, double zMove)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void translateTo(double xPosition, double zPosition)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void translateXTo(double xPosition)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void translateZTo(double zPosition)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void resizeHeight(double height)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void resizeWidth(double width)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void resizeDepth(double depth)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void selectedAction()
    {
        if (isSelected())
        {
            //Set outline
            setStyle("-fx-background-color: blue;");
        }
        else
        {
            //Set outline
            setStyle("-fx-background-color: transparent;");
        }
    }
}
