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
    private final Translate firstArrowTranslate = new Translate();
    private final Translate secondArrowTranslate = new Translate();
    private final Translate textTranslate = new Translate();
    private final Rotate arrowRotate = new Rotate();
    private final Rotate textRotate = new Rotate();

    private final double arrowHeight = 30;
    private final double arrowWidth = 10;
    private final double arrowOffsetFromCorner = 30;

    private LineDirection direction;

    private double normaliseArrowAngle(final double angle)
    {
        double angleToReturn = 0;
        if (angle < 0)
        {
            angleToReturn = -angle - 180;
        } else
        {
            angleToReturn = -angle;
        }
//        steno.info("Rotate in " + angle + " out " + angleToReturn);

        return angleToReturn;
    }

    private double normaliseTextAngle(final double angle)
    {
        double angleToReturn = 0;
        if (angle >= -90 && angle <= 0)
        {
            angleToReturn = -angle - 90;
        } else
        {
            angleToReturn = -angle - 270;
        }
//        steno.info("Rotate in " + angle + " out " + angleToReturn);

        return angleToReturn;
    }

    public enum LineDirection
    {

        HORIZONTAL, VERTICAL, FORWARD_BACK
    }

    public void initialise(ModelContainer modelContainer, LineDirection direction)
    {
        this.direction = direction;
        dimensionText.getStyleClass().add("dimension-label");
        dimensionText.getTransforms().addAll(textTranslate, textRotate);

        dimensionLine.getStyleClass().add("dimension-line");

        upArrow.getStyleClass().add("dimension-arrow");
        downArrow.getStyleClass().add("dimension-arrow");

        downArrow.getPoints().setAll(0d, 0d,
                                     arrowWidth / 2, -arrowHeight,
                                     -arrowWidth / 2, -arrowHeight);
        downArrow.getTransforms().addAll(secondArrowTranslate, arrowRotate);
        arrowRotate.setPivotX(0);
        arrowRotate.setPivotY(0);

        upArrow.getPoints().setAll(0d, 0d,
                                   arrowWidth / 2, arrowHeight,
                                   -arrowWidth / 2, arrowHeight);
        upArrow.getTransforms().addAll(firstArrowTranslate, arrowRotate);

        getChildren().addAll(upArrow, downArrow, dimensionLine, dimensionText);

        setMouseTransparent(true);

        updateArrowAndTextPosition(modelContainer.getScreenExtents(),
                                   modelContainer.getTransformedHeight(),
                                   modelContainer.getTransformedWidth(),
                                   modelContainer.getTransformedDepth());
    }

    @Override
    public void screenExtentsChanged(ScreenExtentsProvider screenExtentsProvider)
    {
        updateArrowAndTextPosition(screenExtentsProvider.getScreenExtents(),
                                   screenExtentsProvider.getTransformedHeight(),
                                   screenExtentsProvider.getTransformedWidth(),
                                   screenExtentsProvider.getTransformedDepth());
    }

    private void updateArrowAndTextPosition(final ScreenExtents extents,
        final double transformedHeight,
        final double transformedWidth,
        final double transformedDepth)
    {
        if (direction == LineDirection.VERTICAL)
        {
            dimensionText.setText(String.
                format("%.2f", transformedHeight));

            Edge heightEdge = extents.heightEdges[0];
            for (int edgeIndex = 1; edgeIndex < extents.heightEdges.length; edgeIndex++)
            {
                if (extents.heightEdges[edgeIndex].getFirstPoint().getX() < heightEdge.
                    getFirstPoint().getX())
                {
                    heightEdge = extents.heightEdges[edgeIndex];
                }
            }

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

            if (topPoint != null
                && bottomPoint != null)
            {
                double opposite = topPoint.getX() - bottomPoint.getX();
                double adjacent = topPoint.getY() - bottomPoint.getY();
                double theta = Math.atan(opposite / adjacent);
                double angle = theta * MathUtils.RAD_TO_DEG;

                dimensionLine.setStartX(bottomPoint.getX());
                dimensionLine.setStartY(bottomPoint.getY());
                dimensionLine.setEndX(topPoint.getX());
                dimensionLine.setEndY(topPoint.getY());

                textTranslate.setX(midPoint.getX());
                textTranslate.setY(midPoint.getY());
                textRotate.setAngle(-angle);

                firstArrowTranslate.setX(topPoint.getX());
                firstArrowTranslate.setY(topPoint.getY());

                secondArrowTranslate.setX(bottomPoint.getX());
                secondArrowTranslate.setY(bottomPoint.getY());
                arrowRotate.setAngle(normaliseArrowAngle(angle));
            }
        } else if (direction == LineDirection.HORIZONTAL)
        {
            dimensionText.setText(String.
                format("%.2f", transformedWidth));

            Edge widthEdge = extents.widthEdges[0];
            if (extents.widthEdges[1].getFirstPoint().getY()
                > widthEdge.getFirstPoint().getY()
                && extents.widthEdges[1].getSecondPoint().getY()
                > widthEdge.getSecondPoint().getY())
            {
                widthEdge = extents.widthEdges[1];
            }

            Point2D leftHorizontalPoint = null;
            Point2D rightHorizontalPoint = null;

            if (widthEdge.getFirstPoint().getX() < widthEdge.getSecondPoint().getX())
            {
                leftHorizontalPoint = widthEdge.getFirstPoint();
                rightHorizontalPoint = widthEdge.getSecondPoint();
            } else
            {
                leftHorizontalPoint = widthEdge.getSecondPoint();
                rightHorizontalPoint = widthEdge.getFirstPoint();
            }

            Point2D midpointScreen = leftHorizontalPoint.midpoint(rightHorizontalPoint);
            Point2D midPoint = screenToLocal(midpointScreen);

            Point2D leftPoint = screenToLocal(leftHorizontalPoint);
            Point2D rightPoint = screenToLocal(rightHorizontalPoint);

            if (leftPoint != null
                && rightPoint != null)
            {
                double opposite = leftPoint.getX() - rightPoint.getX();
                double adjacent = leftPoint.getY() - rightPoint.getY();
                double theta = Math.atan(opposite / adjacent);
                double angle = theta * MathUtils.RAD_TO_DEG;

                dimensionLine.setStartX(rightPoint.getX());
                dimensionLine.setStartY(rightPoint.getY());
                dimensionLine.setEndX(leftPoint.getX());
                dimensionLine.setEndY(leftPoint.getY());

                textTranslate.setX(midPoint.getX());
                textTranslate.setY(midPoint.getY());
                steno.info("Text rotate");
                steno.info("Input " + (angle - 90));
                textRotate.setAngle(normaliseTextAngle(angle));
                steno.info("Normalised " + normaliseTextAngle(angle));
                steno.info("<<<<<<<<<<<");

                firstArrowTranslate.setX(leftPoint.getX());
                firstArrowTranslate.setY(leftPoint.getY());

                secondArrowTranslate.setX(rightPoint.getX());
                secondArrowTranslate.setY(rightPoint.getY());

                double angleToRotateArrow = normaliseArrowAngle(angle);
                arrowRotate.setAngle(angleToRotateArrow);
            }
        } else if (direction == LineDirection.FORWARD_BACK)
        {
            dimensionText.setText(String.
                format("%.2f", transformedDepth));

            Edge depthEdge = extents.depthEdges[0];
            if (extents.depthEdges[1].getFirstPoint().getY()
                > depthEdge.getFirstPoint().getY()
                && extents.depthEdges[1].getSecondPoint().getY()
                > depthEdge.getSecondPoint().getY())
            {
                depthEdge = extents.depthEdges[1];
            }

            Point2D backHorizontalPoint = null;
            Point2D frontHorizontalPoint = null;

            if (depthEdge.getFirstPoint().getX() < depthEdge.getSecondPoint().getX())
            {
                backHorizontalPoint = depthEdge.getFirstPoint();
                frontHorizontalPoint = depthEdge.getSecondPoint();
            } else
            {
                backHorizontalPoint = depthEdge.getSecondPoint();
                frontHorizontalPoint = depthEdge.getFirstPoint();
            }

            Point2D midpointScreen = backHorizontalPoint.midpoint(frontHorizontalPoint);
            Point2D midPoint = screenToLocal(midpointScreen);

            Point2D backPoint = screenToLocal(backHorizontalPoint);
            Point2D frontPoint = screenToLocal(frontHorizontalPoint);

            if (backPoint != null
                && frontPoint != null)
            {
                double opposite = backPoint.getX() - frontPoint.getX();
                double adjacent = backPoint.getY() - frontPoint.getY();
                double theta = Math.atan(opposite / adjacent);
                double angle = theta * MathUtils.RAD_TO_DEG;

                dimensionLine.setStartX(frontPoint.getX());
                dimensionLine.setStartY(frontPoint.getY());
                dimensionLine.setEndX(backPoint.getX());
                dimensionLine.setEndY(backPoint.getY());

                textTranslate.setX(midPoint.getX());
                textTranslate.setY(midPoint.getY());
                textRotate.setAngle(normaliseArrowAngle(angle - 90));

                firstArrowTranslate.setX(backPoint.getX());
                firstArrowTranslate.setY(backPoint.getY());

                secondArrowTranslate.setX(frontPoint.getX());
                secondArrowTranslate.setY(frontPoint.getY());
                arrowRotate.setAngle(normaliseArrowAngle(angle));
            }
        }
    }
}
