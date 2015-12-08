package celtech.coreUI.visualisation;

import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.modelcontrol.ModelContainer;
import static celtech.utils.Math.MathUtils.RAD_TO_DEG;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
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
    private final RestrictedNumberField dimensionLabel = new RestrictedNumberField();
    private final Line dimensionLine = new Line();
    private final Polygon upArrow = new Polygon();
    private final Polygon downArrow = new Polygon();
    private final Translate firstArrowTranslate = new Translate();
    private final Translate secondArrowTranslate = new Translate();
    private final Translate textTranslate = new Translate();
    private final Rotate arrowRotate = new Rotate();
    private final Rotate textRotate = new Rotate();

    private final double arrowHeight = 18;
    private final double arrowWidth = 8;
    private final double arrowOffsetFromCorner = 30;

    private LineDirection direction;
    private Set<ModelContainer> modelContainerSet;
    private UndoableProject undoableproject;

    private double normaliseArrowAngle(final double angle)
    {
        double angleToReturn = 0;
//        if (angle < 0)
//        {
//            angleToReturn = -angle - 180;
//        } else
        if (angle < 0)
        {
            angleToReturn = angle;
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

    private ChangeListener<Number> dimensionEntryChangeListener = new ChangeListener<Number>()
    {

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
        {
            switch (direction)
            {
                case FORWARD_BACK:
                    undoableproject.resizeModelsDepth(modelContainerSet, newValue.floatValue());
                    break;
                case HORIZONTAL:
                    undoableproject.resizeModelsWidth(modelContainerSet, newValue.floatValue());
                    break;
                case VERTICAL:
                    undoableproject.resizeModelsHeight(modelContainerSet, newValue.floatValue());
                    break;
            }
            steno.info("Changed to " + newValue);
        }
    };

    public enum LineDirection
    {

        HORIZONTAL, VERTICAL, FORWARD_BACK
    }

    public void initialise(Project project, ModelContainer modelContainer, LineDirection direction)
    {
        undoableproject = new UndoableProject(project);
        modelContainerSet = new HashSet<>();
        modelContainerSet.add(modelContainer);
        
        this.direction = direction;
        
        dimensionLabel.getTransforms().addAll(textTranslate, textRotate);

        dimensionLabel.getStyleClass().add("dimension-label");
        dimensionLabel.setAllowNegative(false);
        dimensionLabel.setAllowedDecimalPlaces(2);
        dimensionLabel.floatValueProperty().addListener(dimensionEntryChangeListener);

        dimensionLine.setStroke(Color.WHITE);
        upArrow.setFill(Color.WHITE);
        downArrow.setFill(Color.WHITE);

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

        getChildren().addAll(upArrow, downArrow, dimensionLine);

        upArrow.setMouseTransparent(true);
        downArrow.setMouseTransparent(true);
        dimensionLine.setMouseTransparent(true);
        setPickOnBounds(false);

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
            dimensionLabel.setText(String.
                    format("%.1f", transformedHeight));

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
                double xComponent = topPoint.getX() - bottomPoint.getX();
                double yComponent = topPoint.getY() - bottomPoint.getY();
                double lineLength = Math.sqrt((xComponent * xComponent) + (yComponent * yComponent));
                double angle = (RAD_TO_DEG * Math.atan2(xComponent, -yComponent));

                if (lineLength < arrowHeight * 2 + 5)
                {
                    upArrow.setVisible(false);
                    downArrow.setVisible(false);
                } else
                {
                    upArrow.setVisible(true);
                    downArrow.setVisible(true);
                }

                dimensionLine.setStartX(bottomPoint.getX());
                dimensionLine.setStartY(bottomPoint.getY());
                dimensionLine.setEndX(topPoint.getX());
                dimensionLine.setEndY(topPoint.getY());

                textTranslate.setX(midPoint.getX() - dimensionLabel.getBoundsInLocal().getWidth() / 2.0);
                textTranslate.setY(midPoint.getY() - dimensionLabel.getBoundsInLocal().getHeight() / 2.0);
//                textRotate.setAngle(-angle);

                firstArrowTranslate.setX(topPoint.getX());
                firstArrowTranslate.setY(topPoint.getY());

                secondArrowTranslate.setX(bottomPoint.getX());
                secondArrowTranslate.setY(bottomPoint.getY());
                arrowRotate.setAngle(normaliseArrowAngle(angle));
            }
        } else if (direction == LineDirection.HORIZONTAL)
        {
            dimensionLabel.setText(String.
                    format("%.1f", transformedWidth));

            Edge widthEdge = extents.widthEdges[0];
            if (extents.widthEdges[1].getFirstPoint().getY()
                    > widthEdge.getFirstPoint().getY()
                    && extents.widthEdges[1].getSecondPoint().getY()
                    > widthEdge.getSecondPoint().getY())
            {
                widthEdge = extents.widthEdges[1];
            }

            Point2D leftHorizontalPoint = widthEdge.getFirstPoint();
            Point2D rightHorizontalPoint = widthEdge.getSecondPoint();

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
                double xComponent = rightPoint.getX() - leftPoint.getX();
                double yComponent = rightPoint.getY() - leftPoint.getY();
                double lineLength = Math.sqrt((xComponent * xComponent) + (yComponent * yComponent));
                double angle = (RAD_TO_DEG * Math.atan2(xComponent, -yComponent)) - 180;

                if (lineLength < arrowHeight * 2 + 5)
                {
                    upArrow.setVisible(false);
                    downArrow.setVisible(false);
                } else
                {
                    upArrow.setVisible(true);
                    downArrow.setVisible(true);
                }

                dimensionLine.setStartX(rightPoint.getX());
                dimensionLine.setStartY(rightPoint.getY());
                dimensionLine.setEndX(leftPoint.getX());
                dimensionLine.setEndY(leftPoint.getY());

                textTranslate.setX(midPoint.getX() - dimensionLabel.getBoundsInLocal().getWidth() / 2.0);
                textTranslate.setY(midPoint.getY() - dimensionLabel.getBoundsInLocal().getHeight() / 2.0);
//                textTranslate.setY(midPoint.getY() + dimensionLabel.getBoundsInLocal().getHeight() / 2.0);

                firstArrowTranslate.setX(leftPoint.getX());
                firstArrowTranslate.setY(leftPoint.getY());

                secondArrowTranslate.setX(rightPoint.getX());
                secondArrowTranslate.setY(rightPoint.getY());

                arrowRotate.setAngle(angle);
            }
        } else if (direction == LineDirection.FORWARD_BACK)
        {
            dimensionLabel.setText(String.
                    format("%.1f", transformedDepth));

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
                double xComponent = backPoint.getX() - frontPoint.getX();
                double yComponent = backPoint.getY() - frontPoint.getY();
                double lineLength = Math.sqrt((xComponent * xComponent) + (yComponent * yComponent));
                double angle = (RAD_TO_DEG * Math.atan2(xComponent, -yComponent));

                if (lineLength < arrowHeight * 2 + 5)
                {
                    upArrow.setVisible(false);
                    downArrow.setVisible(false);
                } else
                {
                    upArrow.setVisible(true);
                    downArrow.setVisible(true);
                }

                dimensionLine.setStartX(frontPoint.getX());
                dimensionLine.setStartY(frontPoint.getY());
                dimensionLine.setEndX(backPoint.getX());
                dimensionLine.setEndY(backPoint.getY());

                textTranslate.setX(midPoint.getX() - dimensionLabel.getBoundsInLocal().getWidth() / 2.0);
                textTranslate.setY(midPoint.getY() - dimensionLabel.getBoundsInLocal().getHeight() / 2.0);

                firstArrowTranslate.setX(backPoint.getX());
                firstArrowTranslate.setY(backPoint.getY());

                secondArrowTranslate.setX(frontPoint.getX());
                secondArrowTranslate.setY(frontPoint.getY());
                arrowRotate.setAngle(normaliseArrowAngle(angle));
            }
        }
    }
    
    public RestrictedNumberField getDimensionLabel()
    {
        return dimensionLabel;
    }
}
