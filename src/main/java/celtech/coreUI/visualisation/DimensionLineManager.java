package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.layout.Pane;

/**
 *
 * @author Ian
 */
public class DimensionLineManager
{

    private final Pane paneToAddDimensionsTo;
    private final Project associatedProject;
    private final Map<String, DimensionLine> dimensionLines = new HashMap<>();

    public DimensionLineManager(Pane paneToAddDimensionsTo, Project project)
    {
        this.paneToAddDimensionsTo = paneToAddDimensionsTo;
        this.associatedProject = project;

        Lookup.getProjectGUIState(project).getSelectedModelContainers().addListener(
            new SelectedModelContainers.SelectedModelContainersListener()
            {

                @Override
                public void whenAdded(ModelContainer modelContainer)
                {
                    DimensionLine newDimensionLine = new DimensionLine(modelContainer);
                    modelContainer.addScreenExtentsChangeListener(newDimensionLine);
                    paneToAddDimensionsTo.getChildren().add(newDimensionLine);
                    dimensionLines.put(modelContainer.getId(), newDimensionLine);
                }

                @Override
                public void whenRemoved(ModelContainer modelContainer)
                {
                    DimensionLine dimensionLineToRemove = dimensionLines.get(modelContainer.
                        getId());
                    modelContainer.removeScreenExtentsChangeListener(dimensionLineToRemove);
                    paneToAddDimensionsTo.getChildren().remove(dimensionLineToRemove);
                    dimensionLines.remove(modelContainer.getId());
                }
            });
    }
}
