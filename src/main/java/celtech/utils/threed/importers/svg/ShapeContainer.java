package celtech.utils.threed.importers.svg;

import celtech.coreUI.visualisation.Edge;
import celtech.coreUI.visualisation.ScreenExtents;
import celtech.modelcontrol.ItemState;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.modelcontrol.ResizeableTwoD;
import celtech.modelcontrol.ScaleableTwoD;
import celtech.modelcontrol.TranslateableTwoD;
import celtech.modelcontrol.TwoDItemState;
import celtech.roboxbase.utils.twod.ShapeToWorldTransformer;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 *
 * @author ianhudson
 */
public class ShapeContainer extends ProjectifiableThing implements Serializable,
        ScaleableTwoD,
        TranslateableTwoD,
        ResizeableTwoD,
        ShapeToWorldTransformer
{

    private static final long serialVersionUID = 1L;

    private List<Shape> shapes = new ArrayList<>();

    private final Scale scale = new Scale();
    private final Translate translation = new Translate();
    private final Rotate rotation = new Rotate();

    public ShapeContainer()
    {
        super();
        initialise();
    }

    public ShapeContainer(File modelFile)
    {
        super(modelFile);
        initialise();
    }

    public ShapeContainer(String name, List<Shape> shapes)
    {
        super();
        initialise();
        setModelName(name);

        if (shapes.size() > 1)
        {
            Group shapeGroup = new Group();
            shapeGroup.getChildren().addAll(shapes);
            this.getChildren().add(shapeGroup);
        } else
        {
            this.getChildren().add(shapes.get(0));
        }
        this.shapes.addAll(shapes);
    }

    public ShapeContainer(String name, Shape shape)
    {
        super();
        initialise();
        setModelName(name);

        this.getChildren().add(shape);
        this.shapes.add(shape);
    }

    private void initialise()
    {
        this.getTransforms().addAll(rotation, scale, translation);
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
    public void translateBy(double xMove, double yMove)
    {
        translation.setX(translation.getX() + xMove);
        translation.setY(translation.getY() + yMove);
        notifyScreenExtentsChange();
    }

    @Override
    public void translateTo(double xPosition, double yPosition)
    {
        translation.setX(xPosition);
        translation.setY(yPosition);
        notifyScreenExtentsChange();
    }

    @Override
    public void translateXTo(double xPosition)
    {
        translation.setX(xPosition);
        notifyScreenExtentsChange();
    }

    @Override
    public void translateZTo(double yPosition)
    {
        translation.setY(yPosition);
        notifyScreenExtentsChange();
    }

    @Override
    public void resizeHeight(double height)
    {
        Bounds bounds = getBoundsInLocal();

        double currentHeight = bounds.getHeight();

        double newScale = height / currentHeight;
        setYScale(newScale);

        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    @Override
    public void resizeWidth(double width)
    {
        Bounds bounds = getBoundsInLocal();

        double originalWidth = bounds.getWidth();

        double newScale = width / originalWidth;
        setXScale(newScale);

        notifyShapeChange();
        notifyScreenExtentsChange();
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
        boolean extentsChanged = false;

        Bounds localBounds = getBoundsInLocal();

        double minX = localBounds.getMinX();
        double maxX = localBounds.getMaxX();
        double minY = localBounds.getMinY();
        double maxY = localBounds.getMaxY();

        Point2D leftBottom = localToScreen(minX, maxY);
        Point2D rightBottom = localToScreen(maxX, maxY);
        Point2D leftTop = localToScreen(minX, minY);
        Point2D rightTop = localToScreen(maxX, minY);

        ScreenExtents lastExtents = extents;
        if (extents == null && leftBottom != null)
        {
            extents = new ScreenExtents();
        }

        if (extents != null && leftBottom != null)
        {
            extents.heightEdges.clear();
            extents.heightEdges.add(0, new Edge(leftBottom, leftTop));
            extents.heightEdges.add(1, new Edge(rightBottom, rightTop));

            extents.widthEdges.clear();
            extents.widthEdges.add(0, new Edge(leftBottom, rightBottom));
            extents.widthEdges.add(1, new Edge(leftTop, rightTop));

            extents.recalculateMaxMin();
        }

        if (extents != null
                && !extents.equals(lastExtents))
        {
            extentsChanged = true;
        }

        return extentsChanged;
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
        return getBoundsInParent().getMinY() + getBoundsInParent().getHeight() / 2.0;
    }

    @Override
    protected void printVolumeBoundsUpdated()
    {
    }

    @Override
    public void checkOffBed()
    {
    }

    @Override
    public Point2D transformShapeToRealWorldCoordinates(float vertexX, float vertexY)
    {
        return bed.sceneToLocal(localToScene(vertexX, vertexY));
    }

    public List<Shape> getShapes()
    {
        return shapes;
    }
}
