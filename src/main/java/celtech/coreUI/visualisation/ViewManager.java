package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.modelcontrol.ProjectifiableThing;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 *
 * @author Ian
 */
public abstract class ViewManager implements Project.ProjectChangesListener
{

    protected Project project = null;
    //This holds the current selection in the associated project
    protected ProjectSelection projectSelection = null;
    protected UndoableProject undoableProject = null;
    protected ObservableList<ProjectifiableThing> loadedModels;

    private final List<ViewChangeListener> viewChangeListeners = new ArrayList<>();

    public abstract Node getDisplayableComponent();

    protected abstract double getViewPortDistance();

    protected abstract void addModelAction(ProjectifiableThing projectifiableThing);

    public void associateWithProject(Project project)
    {
        this.project = project;
        this.undoableProject = new UndoableProject(project);
        projectSelection = Lookup.getProjectGUIState(project).getProjectSelection();
        loadedModels = project.getTopLevelThings();

        /**
         * Listen for adding and removing of models from the project
         */
        project.addProjectChangesListener(this);

        for (ProjectifiableThing projectifiableThing : project.getAllModels())
        {
            addModelAction(projectifiableThing);
        }
    }

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

        if (projectSelection != null)
        {
            projectSelection.getSelectedModelsSnapshot().stream().forEach((projectifiableThing) ->
            {
                projectifiableThing.viewOfYouHasChanged(viewportDistance);
            });
        }
        
        viewChangeListeners.stream().forEach((listener) ->
        {
            listener.viewOfYouHasChanged(viewportDistance);
        });
    }
}
