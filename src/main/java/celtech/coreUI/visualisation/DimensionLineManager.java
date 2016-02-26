package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.coreUI.visualisation.DimensionLine.LineDirection;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ProjectifiableThing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;

/**
 *
 * @author Ian
 */
public class DimensionLineManager
{

    private final Map<ModelContainer, List<DimensionLine>> dimensionLines = new HashMap<>();

    private final ChangeListener<Boolean> dragModeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        for (List<DimensionLine> dimensionLineList : dimensionLines.values())
        {
            for (DimensionLine dimensionLine : dimensionLineList)
            {
                dimensionLine.getDimensionLabel().setVisible(newValue == true);
            }
        }
    };

    public DimensionLineManager(Pane paneToAddDimensionsTo, Project project, ReadOnlyBooleanProperty hideDimensionsProperty)
    {
        hideDimensionsProperty.addListener(dragModeListener);

        Lookup.getProjectGUIState(project).getProjectSelection().addListener(
                new ProjectSelection.SelectedModelContainersListener()
                {

                    @Override
                    public void whenAdded(ProjectifiableThing projectifiableThing)
                    {
                        if (projectifiableThing instanceof ModelContainer)
                        {
                            ModelContainer modelContainer = (ModelContainer) projectifiableThing;
                            ArrayList<DimensionLine> lineList = new ArrayList<>();
                            DimensionLine verticalDimension = new DimensionLine();
                            modelContainer.addScreenExtentsChangeListener(verticalDimension);
                            paneToAddDimensionsTo.getChildren().add(verticalDimension);
                            verticalDimension.initialise(project,
                                    modelContainer,
                                    LineDirection.VERTICAL);
                            lineList.add(verticalDimension);

                            DimensionLine horizontalDimension = new DimensionLine();
                            modelContainer.addScreenExtentsChangeListener(horizontalDimension);
                            paneToAddDimensionsTo.getChildren().add(horizontalDimension);
                            horizontalDimension.initialise(project, modelContainer, LineDirection.HORIZONTAL);
                            lineList.add(horizontalDimension);

                            DimensionLine frontBackDimension = new DimensionLine();
                            modelContainer.addScreenExtentsChangeListener(frontBackDimension);
                            paneToAddDimensionsTo.getChildren().add(frontBackDimension);
                            frontBackDimension.initialise(project, modelContainer, LineDirection.FORWARD_BACK);
                            lineList.add(frontBackDimension);

                            paneToAddDimensionsTo.getChildren().add(verticalDimension.getDimensionLabel());
                            paneToAddDimensionsTo.getChildren().add(horizontalDimension.getDimensionLabel());
                            paneToAddDimensionsTo.getChildren().add(frontBackDimension.getDimensionLabel());

                            dimensionLines.put(modelContainer, lineList);

                            Platform.runLater(() ->
                                    {
                                        verticalDimension.screenExtentsChanged(modelContainer);
                                        horizontalDimension.screenExtentsChanged(modelContainer);
                                        frontBackDimension.screenExtentsChanged(modelContainer);
                            });
                        }
                    }

                    @Override
                    public void whenRemoved(ProjectifiableThing projectifiableThing)
                    {
                        if (projectifiableThing instanceof ModelContainer)
                        {
                            ModelContainer modelContainer = (ModelContainer) projectifiableThing;

                            List<DimensionLine> dimensionLinesToRemove = dimensionLines
                            .get(modelContainer);
                            dimensionLinesToRemove.forEach(line ->
                                    {
                                        modelContainer.removeScreenExtentsChangeListener(line);
                                        paneToAddDimensionsTo.getChildren().remove(line);
                                        paneToAddDimensionsTo.getChildren().remove(line.getDimensionLabel());
                            });
                            dimensionLines.remove(modelContainer);
                        }
                    }
                });
    }
}
