package celtech.coreUI.visualisation;

import celtech.modelcontrol.ModelContainer;
import celtech.utils.Math.MathUtils;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
class DimensionLine extends Pane implements ScreenExtentsProvider.ScreenExtentsListener
{

    private Stenographer steno = StenographerFactory.getStenographer(DimensionLine.class.getName());
    private final Text dimensionText = new Text();
    private final Line dimensionLine = new Line();
    private final Polygon upArrow = new Polygon();
    private final Polygon downArrow = new Polygon();
    private final Translate upArrowTranslate = new Translate();
    private final Translate downArrowTranslate = new Translate();
    private final Translate verticalTextTranslate = new Translate();
    private final Rotate verticalArrowRotate = new Rotate();

    private final double arrowHeight = 30;
    private final double arrowWidth = 10;
    private final double arrowOffsetFromCorner = 30;

    public DimensionLine(ModelContainer modelContainer)
    {
        dimensionText.getStyleClass().add("dimension-label");
        dimensionText.getTransforms().addAll(verticalTextTranslate, verticalArrowRotate);
        
        dimensionLine.getStyleClass().add("dimension-line");
        
        upArrow.getStyleClass().add("dimension-arrow");
        downArrow.getStyleClass().add("dimension-arrow");

        downArrow.getPoints().setAll(0d, 0d,
                                     arrowWidth / 2, -arrowHeight,
                                     -arrowWidth / 2, -arrowHeight);
        downArrow.getTransforms().addAll(downArrowTranslate, verticalArrowRotate);
        verticalArrowRotate.setPivotX(0);
        verticalArrowRotate.setPivotY(0);

        upArrow.getPoints().setAll(0d, 0d,
                                   arrowWidth / 2, arrowHeight,
                                   -arrowWidth / 2, arrowHeight);
        upArrow.getTransforms().addAll(upArrowTranslate, verticalArrowRotate);

        getChildren().addAll(upArrow, downArrow, dimensionLine, dimensionText);
    }

    @Override
    public void screenExtentsChanged(ScreenExtentsProvider screenExtentsProvider)
    {
        ScreenExtents extents = screenExtentsProvider.getScreenExtents();

        dimensionText.setText(String.format("%.2f", screenExtentsProvider.getTransformedHeight()));

        Edge heightEdge = extents.heightEdges[0];

        Point2D topVerticalPoint = null;
        Point2D bottomVerticalPoint = null;

        if (heightEdge.getFirstPoint().getY() < heightEdge.getSecondPoint().getY())
        {
            topVerticalPoint = heightEdge.getFirstPoint();
            bottomVerticalPoint = heightEdge.getSecondPoint();
        } else
        {
            topVerticalPoint = heightEdge.getSecondPoint();
            bottomVerticalPoint = heightEdge.getFirstPoint();
        }

        Point2D midpointScreen = topVerticalPoint.midpoint(bottomVerticalPoint);
        Point2D midPoint = screenToLocal(midpointScreen);

        Point2D topPoint = screenToLocal(topVerticalPoint);
        Point2D bottomPoint = screenToLocal(bottomVerticalPoint);

        double opposite = topPoint.getX() - bottomPoint.getX();
        double adjacent = topPoint.getY() - bottomPoint.getY();
        double theta = Math.atan(opposite / adjacent);
        double angle = theta * MathUtils.RAD_TO_DEG;

        dimensionLine.setStartX(bottomPoint.getX());
        dimensionLine.setStartY(bottomPoint.getY());
        dimensionLine.setEndX(topPoint.getX());
        dimensionLine.setEndY(topPoint.getY());

        verticalTextTranslate.setX(midPoint.getX());
        verticalTextTranslate.setY(midPoint.getY());

        upArrowTranslate.setX(topPoint.getX());
        upArrowTranslate.setY(topPoint.getY());

        downArrowTranslate.setX(bottomPoint.getX());
        downArrowTranslate.setY(bottomPoint.getY());
        verticalArrowRotate.setAngle(-angle);
    }
}
