package celtech.coreUI.visualisation.svg;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.shape.SVGPath;

/**
 *
 * @author Ian
 */
public class TextPath extends SVGPath implements PrintableShape
{

    private String textToDisplay = "";
    private GraphicsEnvironment ge;
    private Font fontInUse;
    private FontRenderContext frc;
    private ContextMenu contextMenu = null;
    private final TextPath thisTextPath;

    public TextPath()
    {
        thisTextPath = this;
        ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        updateFont(new Font("Segoe UI", Font.PLAIN, 48));

        setupContextMenu();
    }

    private void setupContextMenu()
    {
        contextMenu = new ContextMenu();

        String cm1Text = "Edit Text";
        String cm2Text = "Change font";

        MenuItem cmItem1 = new MenuItem(cm1Text);
        MenuItem cmItem2 = new MenuItem(cm2Text);

        cmItem1.setOnAction((ActionEvent e) ->
        {
//            String textToDisplay = "Hello";
//            TextPath newPath = new TextPath();
//            newPath.setText(textToDisplay);
//            parts.getChildren().add(newPath);
//
//            Point2D pointToPlaceAt = partsAndBed.screenToLocal(bedContextMenu.getAnchorX(), bedContextMenu.getAnchorY());
//            newPath.setTranslateX(pointToPlaceAt.getX());
//            newPath.setTranslateY(pointToPlaceAt.getY());
        });

        cmItem2.setOnAction((ActionEvent e) ->
        {

        });

        contextMenu.getItems().addAll(cmItem1, cmItem2);

        setOnContextMenuRequested(new EventHandler<ContextMenuEvent>()
        {
            @Override
            public void handle(ContextMenuEvent event)
            {
                Point3D parentPoint = ((TextPath) event.getTarget()).localToParent(event.getPickResult().getIntersectedPoint());
                contextMenu.show(thisTextPath, Side.TOP, parentPoint.getX(), parentPoint.getY());
            }
        });
    }

    private void updateFont(Font fontWeWishToUse)
    {
        fontInUse = fontWeWishToUse;
        frc = new FontRenderContext(fontInUse.getTransform(), true, true);
        updateTextPath();
    }

    private void updateTextPath()
    {
        GlyphVector v = fontInUse.createGlyphVector(frc, textToDisplay);
        Shape s = v.getOutline();
        PathIterator pathIterator = s.getPathIterator(null);

        StringBuilder svgContent = new StringBuilder();

        float[] coords = new float[6];

        while (!pathIterator.isDone())
        {
            int pathType = pathIterator.currentSegment(coords);
            switch (pathType)
            {
                case PathIterator.SEG_CLOSE:
//                    steno.info("Got a close");
                    svgContent.append("z ");
                    break;
                case PathIterator.SEG_CUBICTO:
//                    steno.info("Got a cubic to "
//                            + "x1:" + coords[0]
//                            + "y1:" + coords[1]
//                            + "x2:" + coords[2]
//                            + "y2:" + coords[3]
//                            + "x3:" + coords[4]
//                            + "y3:" + coords[5]);
                    svgContent.append("C");
                    svgContent.append(coords[0]);
                    svgContent.append(" ");
                    svgContent.append(coords[1]);
                    svgContent.append(" ");
                    svgContent.append(coords[2]);
                    svgContent.append(" ");
                    svgContent.append(coords[3]);
                    svgContent.append(" ");
                    svgContent.append(coords[4]);
                    svgContent.append(" ");
                    svgContent.append(coords[5]);
                    svgContent.append(" ");
                    break;
                case PathIterator.SEG_LINETO:
//                    steno.info("Got a line to "
//                            + "x1:" + coords[0]
//                            + "y1:" + coords[1]);
                    svgContent.append("L");
                    svgContent.append(coords[0]);
                    svgContent.append(" ");
                    svgContent.append(coords[1]);
                    svgContent.append(" ");
                    break;
                case PathIterator.SEG_MOVETO:
//                    steno.info("Got a move to "
//                            + "x1:" + coords[0]
//                            + "y1:" + coords[1]);
                    svgContent.append("M");
                    svgContent.append(coords[0]);
                    svgContent.append(" ");
                    svgContent.append(coords[1]);
                    svgContent.append(" ");
                    break;
                case PathIterator.SEG_QUADTO:
//                    steno.info("Got a quad to "
//                            + "x1:" + coords[0]
//                            + "y1:" + coords[1]
//                            + "x2:" + coords[2]
//                            + "y2:" + coords[3]);
                    svgContent.append("Q");
                    svgContent.append(coords[0]);
                    svgContent.append(" ");
                    svgContent.append(coords[1]);
                    svgContent.append(" ");
                    svgContent.append(coords[2]);
                    svgContent.append(" ");
                    svgContent.append(coords[3]);
                    svgContent.append(" ");
                    break;
            }
            pathIterator.next();
        }

        this.setContent(svgContent.toString());
    }

    public void setFont()
    {

    }

    public void setText(String newText)
    {
        textToDisplay = newText;
        updateTextPath();
    }

    @Override
    public void relativeTranslate(double x, double y)
    {
        setTranslateX(getTranslateX() + x);
        setTranslateY(getTranslateY() + y);
    }

    @Override
    public String getSVGPathContent()
    {
        return getContent();
    }
}
