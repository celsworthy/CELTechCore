package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.Project;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;

/**
 *
 * @author Ian
 */
public abstract class ViewManager
{

    //This holds the current selection in the associated project
    protected final ProjectSelection projectSelection;

    private final List<ViewChangeListener> viewChangeListeners = new ArrayList<>();

    public ViewManager(Project project)
    {
        projectSelection = Lookup.getProjectGUIState(project).getProjectSelection();
    }

    public abstract Node getDisplayableComponent();

    protected abstract double getViewPortDistance();

    public void addViewPortChangeListener(ViewChangeListener listener)
    {
        if (!viewChangeListeners.contains(listener))
        {
            viewChangeListeners.add(listener);
        }
    }

    protected void notifyModelsAndListenersOfViewPortChange()
    {
        double viewportDistance = getViewPortDistance();

        projectSelection.getSelectedModelsSnapshot().stream().forEach((projectifiableThing) ->
        {
            projectifiableThing.viewOfYouHasChanged(viewportDistance);
        });

        viewChangeListeners.stream().forEach((listener) ->
        {
            listener.viewOfYouHasChanged(viewportDistance);
        });
    }
}
