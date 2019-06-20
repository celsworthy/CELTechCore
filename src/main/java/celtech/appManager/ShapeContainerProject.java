package celtech.appManager;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.ModelContainerProjectFile;
import celtech.configuration.fileRepresentation.ProjectFile;
import celtech.configuration.fileRepresentation.ShapeContainerProjectFile;
import celtech.modelcontrol.Groupable;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.modelcontrol.RotatableTwoD;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.utils.threed.importers.svg.ShapeContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class ShapeContainerProject extends Project
{

    private static final Stenographer steno = StenographerFactory.getStenographer(ShapeContainerProject.class.getName());
    private int version = -1;

    private final StylusSettings stylusSettings = new StylusSettings();

    public ShapeContainerProject()
    {
        super();
        stylusSettings.getDataChanged().addListener(
        (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
        {
            steno.info("Stylus settings changed");
            projectModified();
            fireWhenPrinterSettingsChanged(printerSettings);
        });
    }

    @Override
    protected void initialise()
    {
        setMode(ProjectMode.SVG);
    }

    @Override
    protected void save(String basePath)
    {
        if (topLevelThings.size() > 0)
        {
            try
            {
                ProjectFile projectFile = new ShapeContainerProjectFile();
                projectFile.populateFromProject(this);
                File file = new File(basePath + ApplicationConfiguration.projectFileExtension);
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                mapper.writeValue(file, projectFile);
                saveModels(basePath + ApplicationConfiguration.projectModelsFileExtension);
            } catch (FileNotFoundException ex)
            {
                steno.exception("Failed to save project state", ex);
            } catch (IOException ex)
            {
                steno.exception(
                        "Couldn't write project state to file for project "
                        + projectNameProperty.get(), ex);
            }
        }

    }

    @Override
    public void addModel(ProjectifiableThing projectifiableThing)
    {
        if (projectifiableThing instanceof ShapeContainer)
        {
            ShapeContainer modelContainer = (ShapeContainer) projectifiableThing;
            topLevelThings.add(modelContainer);
            projectModified();
            fireWhenModelAdded(modelContainer);
        }
    }
    
    public static void saveProject(ModelContainerProject project)
    {
        String basePath = ApplicationConfiguration.getProjectDirectory() + File.separator
                + project.getProjectName();
        project.save(basePath);
    }
    
    public String getProjectLocation() {
        return ApplicationConfiguration.getProjectDirectory() 
                + File.separator
                + projectNameProperty.get() 
                + File.separator;
    }

    private Set<ShapeContainer> getModelsHoldingShapes()
    {
        Set<ShapeContainer> modelsHoldingShapes = new HashSet<>();
        for (ProjectifiableThing model : topLevelThings)
        {
            modelsHoldingShapes.add((ShapeContainer) model);
        }
        return modelsHoldingShapes;
    }

    private void saveModels(String path) throws IOException
    {
        ObjectOutputStream modelsOutput = new ObjectOutputStream(new FileOutputStream(path));

        Set<ShapeContainer> modelsHoldingShapes = getModelsHoldingShapes();

        modelsOutput.writeInt(modelsHoldingShapes.size());
        for (ShapeContainer modelHoldingShape : modelsHoldingShapes)
        {
            modelsOutput.writeObject(modelHoldingShape);
        }
    }

    private void fireWhenModelAdded(ShapeContainer modelContainer)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelAdded(modelContainer);
        }
    }

    @Override
    public void removeModels(Set<ProjectifiableThing> projectifiableThings)
    {
        Set<ShapeContainer> modelContainers = (Set) projectifiableThings;

        for (ShapeContainer modelContainer : modelContainers)
        {
            assert modelContainer != null;
        }

        topLevelThings.removeAll(modelContainers);

        projectModified();
        fireWhenModelsRemoved(projectifiableThings);
    }
    
    private void fireWhenModelsRemoved(Set<ProjectifiableThing> modelContainers)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelsRemoved(modelContainers);
        }
    }


    @Override
    public void autoLayout()
    {
    }

    @Override
    public Set<ProjectifiableThing> getAllModels()
    {
        Set<ProjectifiableThing> allModelContainers = new HashSet<>();
        for (ProjectifiableThing loadedModel : topLevelThings)
        {
            allModelContainers.add(loadedModel);
        }
        return allModelContainers;
    }

    @Override
    protected void fireWhenModelsTransformed(Set<ProjectifiableThing> projectifiableThings)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelsTransformed(projectifiableThings);
        }
    }

    @Override
    protected void fireWhenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenPrinterSettingsChanged(printerSettings);
        }
    }

    @Override
    protected void load(ProjectFile projectFile, String basePath) throws ProjectLoadException
    {
        suppressProjectChanged = true;

        if (projectFile instanceof ShapeContainerProjectFile)
        {
            try
            {
                version = projectFile.getVersion();
                projectNameProperty.set(projectFile.getProjectName());
                lastModifiedDate.set(projectFile.getLastModifiedDate());
                lastPrintJobID = projectFile.getLastPrintJobID();
                projectNameModified = projectFile.isProjectNameModified();

                loadModels(basePath);

            } catch (IOException ex)
            {
                steno.exception("Failed to load project " + basePath, ex);
            } catch (ClassNotFoundException ex)
            {
                steno.exception("Failed to load project " + basePath, ex);
            }
        }

        suppressProjectChanged = false;
    }

    private void loadModels(String basePath) throws IOException, ClassNotFoundException
    {
        FileInputStream fileInputStream = new FileInputStream(basePath
                + ApplicationConfiguration.projectModelsFileExtension);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        ObjectInputStream modelsInput = new ObjectInputStream(bufferedInputStream);
        int numModels = modelsInput.readInt();

        for (int i = 0; i < numModels; i++)
        {
            ShapeContainer modelContainer = (ShapeContainer) modelsInput.readObject();
            addModel(modelContainer);
        }
    }

    @Override
    protected void checkNotAlreadyInGroup(Set<Groupable> modelContainers)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ModelGroup createNewGroupAndAddModelListeners(Set<Groupable> modelContainers)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void autoLayout(List<ProjectifiableThing> thingsToLayout)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void rotateTurnModels(Set<RotatableTwoD> modelContainers, double rotation)
    {
        for (RotatableTwoD model : modelContainers)
        {
            model.setRotationTurn(rotation);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }
    
    
    public StylusSettings getStylusSettings()
    {
        return stylusSettings;
    }
}
