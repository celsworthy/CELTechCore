package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.coreUI.visualisation.DimensionLine.LineDirection;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.utils.threed.importers.svg.ShapeContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;

/**
 *
 * @author Ian
 */
public class DimensionLineManager
{

    private final Map<ProjectifiableThing, List<DimensionLine>> dimensionLines = new HashMap<>();

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
                        boolean addDimensionLines = false;
                        ArrayList<DimensionLine> lineList = new ArrayList<>();
                        DimensionLine verticalDimension = new DimensionLine();
                        DimensionLine frontBackDimension = new DimensionLine();
                        DimensionLine horizontalDimension = new DimensionLine();
                        ScreenExtentsProviderTwoD screenExtentsProviderTwoD = null;
                        ScreenExtentsProviderThreeD screenExtentsProviderThreeD = null;

                        if (projectifiableThing instanceof ScreenExtentsProviderTwoD)
                        {
                            screenExtentsProviderTwoD = (ScreenExtentsProviderTwoD) projectifiableThing;
                            screenExtentsProviderTwoD.addScreenExtentsChangeListener(verticalDimension);
                            paneToAddDimensionsTo.getChildren().add(verticalDimension);
                            verticalDimension.initialise(project,
                                    screenExtentsProviderTwoD,
                                    LineDirection.VERTICAL);
                            lineList.add(verticalDimension);

                            screenExtentsProviderTwoD.addScreenExtentsChangeListener(horizontalDimension);
                            paneToAddDimensionsTo.getChildren().add(horizontalDimension);
                            horizontalDimension.initialise(project, screenExtentsProviderTwoD, LineDirection.HORIZONTAL);
                            lineList.add(horizontalDimension);

                            paneToAddDimensionsTo.getChildren().add(verticalDimension.getDimensionLabel());
                            paneToAddDimensionsTo.getChildren().add(horizontalDimension.getDimensionLabel());
                            addDimensionLines = true;
                        }

                        if (projectifiableThing instanceof ScreenExtentsProviderThreeD)
                        {

                            screenExtentsProviderThreeD = (ScreenExtentsProviderThreeD) projectifiableThing;

                            screenExtentsProviderThreeD.addScreenExtentsChangeListener(frontBackDimension);
                            paneToAddDimensionsTo.getChildren().add(frontBackDimension);
                            frontBackDimension.initialise(project, screenExtentsProviderThreeD, LineDirection.FORWARD_BACK);
                            lineList.add(frontBackDimension);

                            paneToAddDimensionsTo.getChildren().add(frontBackDimension.getDimensionLabel());
                        }

                        if (addDimensionLines)
                        {
                            dimensionLines.put(projectifiableThing, lineList);

                            ScreenExtentsProviderTwoD finalScreenExtentsProviderTwoD = screenExtentsProviderTwoD;
                            ScreenExtentsProviderThreeD finalScreenExtentsProviderThreeD = screenExtentsProviderThreeD;

                            Platform.runLater(() ->
                                    {
                                        verticalDimension.screenExtentsChanged(finalScreenExtentsProviderTwoD);
                                        horizontalDimension.screenExtentsChanged(finalScreenExtentsProviderTwoD);
                                        if (projectifiableThing instanceof ModelContainer)
                                        {
                                            frontBackDimension.screenExtentsChanged(finalScreenExtentsProviderThreeD);
                                        }
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
                }
        );
    }
}
