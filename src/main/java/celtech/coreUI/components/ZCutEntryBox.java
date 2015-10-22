package celtech.coreUI.components;

import celtech.appManager.Project;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.visualisation.Edge;
import celtech.coreUI.visualisation.ScreenExtents;
import celtech.coreUI.visualisation.ScreenExtentsProvider;
import celtech.coreUI.visualisation.ScreenExtentsProvider.ScreenExtentsListener;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.modelcontrol.ModelContainer;
import java.io.IOException;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

/**
 *
 * @author Ian
 */
public class ZCutEntryBox extends HBox implements ScreenExtentsListener
{
    
    @FXML
    private RestrictedNumberField cutHeight;
    
    @FXML
    private void accept(ActionEvent event)
    {
        List<ModelContainer> generatedModels = viewManager.cutModelAt(currentModel, cutHeight.doubleValueProperty().get());
        for (ModelContainer mc : generatedModels)
        {
            project.addModel(mc);
        }
    }
    
    @FXML
    private void cancel(ActionEvent event)
    {
        if (layoutSubmodeProperty != null)
        {
            layoutSubmodeProperty.set(LayoutSubmode.SELECT);
        }
        
        viewManager.clearZCutModelPlane();
    }
    
    private final Node paneInWhichControlResides;
    private final ObjectProperty<LayoutSubmode> layoutSubmodeProperty;
    private final ThreeDViewManager viewManager;
    private ModelContainer currentModel = null;
    private final Project project;
    
    public ZCutEntryBox()
    {
        paneInWhichControlResides = null;
        layoutSubmodeProperty = null;
        viewManager = null;
        project = null;
        loadContent();
    }
    
    public ZCutEntryBox(Node paneInWhichControlResides,
            ObjectProperty<LayoutSubmode> layoutSubmodeProperty,
            ThreeDViewManager viewManager,
            Project project)
    {
        this.paneInWhichControlResides = paneInWhichControlResides;
        this.layoutSubmodeProperty = layoutSubmodeProperty;
        this.viewManager = viewManager;
        this.project = project;
        loadContent();
    }
    
    private void loadContent()
    {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/celtech/resources/fxml/components/ZCutEntryBox.fxml"));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        fxmlLoader.setClassLoader(this.getClass().getClassLoader());
        
        try
        {
            fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }
    }
    
    public void prime(ModelContainer modelContainer)
    {
        currentModel = modelContainer;
        cutHeight.doubleValueProperty().set(modelContainer.getTransformedHeight() / 2);
        
        modelContainer.addScreenExtentsChangeListener(this);
        
        viewManager.showZCutPlane(modelContainer, cutHeight.doubleValueProperty());
    }
    
    @Override
    public void screenExtentsChanged(ScreenExtentsProvider screenExtentsProvider)
    {
        System.out.println("New extents " + screenExtentsProvider.getScreenExtents());
        
        ScreenExtents extents = screenExtentsProvider.getScreenExtents();

        //Half way up
        int yPosition = extents.maxY - ((extents.maxY - extents.minY) / 2);
        
        Bounds screenBoundsOfMe = localToScreen(getBoundsInParent());
        
        int xPosition = extents.minX;
        xPosition -= screenBoundsOfMe.getWidth();
        
        if (xPosition < 0)
        {
            xPosition = 0;
        }

        //Always put this at the left hand edge
        Point2D position = paneInWhichControlResides.screenToLocal(xPosition, yPosition);
        System.out.println("Translating to " + position);
//        setTranslateX(position.getX());
//        setTranslateY(position.getY());
    }
}
