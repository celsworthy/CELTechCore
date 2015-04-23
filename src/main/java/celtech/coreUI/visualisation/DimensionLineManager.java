package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.coreUI.visualisation.DimensionLine.LineDirection;
import celtech.modelcontrol.ModelContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final Map<String, List<DimensionLine>> dimensionLines = new HashMap<>();

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
                    ArrayList<DimensionLine> lineList = new ArrayList<>();
                    DimensionLine verticalDimension = new DimensionLine(modelContainer,
                                                                        LineDirection.VERTICAL);
                    modelContainer.addScreenExtentsChangeListener(verticalDimension);
                    paneToAddDimensionsTo.getChildren().add(verticalDimension);
                    lineList.add(verticalDimension);

                    DimensionLine horizontalDimension = new DimensionLine(modelContainer,
                                                                          LineDirection.HORIZONTAL);
                    modelContainer.addScreenExtentsChangeListener(horizontalDimension);
                    paneToAddDimensionsTo.getChildren().add(horizontalDimension);
                    lineList.add(horizontalDimension);

                    DimensionLine frontBackDimension = new DimensionLine(modelContainer,
                                                                          LineDirection.FORWARD_BACK);
                    modelContainer.addScreenExtentsChangeListener(frontBackDimension);
                    paneToAddDimensionsTo.getChildren().add(frontBackDimension);
                    lineList.add(frontBackDimension);
                    
                    dimensionLines.put(modelContainer.getId(), lineList);
                }

                @Override
                public void whenRemoved(ModelContainer modelContainer)
                {
                    List<DimensionLine> dimensionLinesToRemove = dimensionLines
                    .get(modelContainer.getId());
                    dimensionLinesToRemove.forEach(line ->
                        {
                            modelContainer.removeScreenExtentsChangeListener(line);
                            paneToAddDimensionsTo.getChildren().remove(line);
                    });
                    dimensionLines.remove(modelContainer.getId());
                }
            });
    }
}
