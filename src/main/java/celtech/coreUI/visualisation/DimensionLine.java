package celtech.coreUI.visualisation;

import celtech.modelcontrol.ModelContainer;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;

/**
 *
 * @author Ian
 */
class DimensionLine extends Pane implements ScreenExtentsProvider.ScreenExtentsListener
{
    private final Text dimensionText = new Text();
    private final Line dimensionLine = new Line();
    private final Polygon upArrow = new Polygon();
    private final Polygon downArrow = new Polygon();
    private final double arrowHeight = 30;
    private final double arrowWidth = 10;
    
    public DimensionLine(ModelContainer modelContainer)
    {
        dimensionText.getStyleClass().add("dimension-label");
        dimensionLine.getStyleClass().add("dimension-line");

        upArrow.getPoints().setAll(0d, 0d,
                                   arrowWidth / 2, -arrowHeight,
                                   -arrowWidth / 2, -arrowHeight);
        
        downArrow.getPoints().setAll(0d, 0d,
                                   arrowWidth / 2, arrowHeight,
                                   -arrowWidth / 2, arrowHeight);
        
        getChildren().addAll(upArrow, downArrow);
    }

    @Override
    public void screenExtentsChanged(ScreenExtentsProvider screenExtentsProvider)
    {
        Point2D left = screenExtentsProvider.getScreenExtents();
        dimensionText.setText(String.format("%.2f", screenExtentsProvider.getTransformedHeight()));
        
        Point2D leftInLocal = getParent().screenToLocal(left);
        setTranslateX(leftInLocal.getX());
        setTranslateY(leftInLocal.getY());
        
        upArrow.setTranslateY(-50);
        downArrow.setTranslateY(50);
    }
}
