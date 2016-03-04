package celtech.utils.threed.importers.svg;

import celtech.modelcontrol.ItemState;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.modelcontrol.ResizeableTwoD;
import celtech.modelcontrol.ScaleableTwoD;
import celtech.modelcontrol.TranslateableTwoD;
import celtech.modelcontrol.TwoDItemState;
import celtech.roboxbase.importers.twod.svg.metadata.SVGMetaPart;
import java.io.File;
import java.io.Serializable;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 *
 * @author ianhudson
 */
public class ShapeContainer extends ProjectifiableThing implements Serializable, ScaleableTwoD, TranslateableTwoD, ResizeableTwoD
{

    private static final long serialVersionUID = 1L;

    //Keep these parts for printing later on
    private List<SVGMetaPart> metaparts;

    private final Scale scale = new Scale();
    private final Translate translation = new Translate();
    private final Rotate rotation = new Rotate();

    public ShapeContainer()
    {
        super();
    }

    public ShapeContainer(File modelFile)
    {
        super(modelFile);
        initialise();
    }

    private void initialise()
    {
        this.getTransforms().addAll(rotation, scale, translation);
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
    public ItemState getState()
    {
        return new TwoDItemState(modelId,
                translation.getX(),
                translation.getY(),
                scale.getX(),
                scale.getY(),
                rotation.getAngle());
    }

    @Override
    public void setState(ItemState state)
    {
        if (state instanceof TwoDItemState)
        {
            translation.setX(state.x);
            translation.setY(state.y);

            scale.setX(state.preferredXScale);
            scale.setY(state.preferredYScale);

            rotation.setAngle(state.preferredRotationTurn);
        }
    }

    @Override
    public double getXScale()
    {
        return scale.getX();
    }

    @Override
    public void setXScale(double scaleFactor)
    {
        scale.setX(scaleFactor);
    }

    @Override
    public double getYScale()
    {
        return scale.getY();
    }

    @Override
    public void setYScale(double scaleFactor)
    {
        scale.setY(scaleFactor);
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
    public void translateBy(double xMove, double yMove)
    {
        translation.setX(translation.getX() + xMove);
        translation.setY(translation.getY() + yMove);
    }

    @Override
    public void translateTo(double xPosition, double yPosition)
    {
        translation.setX(xPosition);
        translation.setY(yPosition);
    }

    @Override
    public void translateXTo(double xPosition)
    {
        translation.setX(xPosition);
    }

    @Override
    public void translateZTo(double yPosition)
    {
        translation.setY(yPosition);
    }

    @Override
    public void resizeHeight(double height)
    {
    }

    @Override
    public void resizeWidth(double width)
    {
    }

    @Override
    public void selectedAction()
    {
        if (isSelected())
        {
            //Set outline
            setStyle("-fx-border-color: blue; -fx-border-size: 1px;");
        } else
        {
            //Set outline
            setStyle("-fx-border-color: black;");
        }
    }

    @Override
    protected boolean recalculateScreenExtents()
    {
        //TODO calculate screen extents
        return false;
    }

    @Override
    public double getTransformedHeight()
    {
        return getBoundsInParent().getHeight();
    }

    @Override
    public double getTransformedWidth()
    {
        return getBoundsInParent().getWidth();
    }

    @Override
    public double getScaledWidth()
    {
        return getTransformedWidth();
    }

    @Override
    public double getScaledHeight()
    {
        return getTransformedHeight();
    }

    @Override
    public double getCentreX()
    {
        return getBoundsInParent().getMinX() + getBoundsInParent().getWidth() / 2.0;
    }

    @Override
    public double getCentreY()
    {
        return getBoundsInParent().getMinY() + getBoundsInParent().getHeight()/ 2.0;
    }
}
