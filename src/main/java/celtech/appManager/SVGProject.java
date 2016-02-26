package celtech.appManager;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.ProjectFile;
import celtech.configuration.fileRepresentation.SVGProjectFile;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.utils.threed.importers.svg.RenderableSVG;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class SVGProject extends Project
{

    private static final Stenographer steno = StenographerFactory.getStenographer(SVGProject.class.getName());

    @Override
    protected void initialise()
    {
    }

    @Override
    protected void save(String basePath)
    {
    }

    @Override
    public void addModel(ProjectifiableThing projectifiableThing)
    {
        if (projectifiableThing instanceof RenderableSVG)
        {
            RenderableSVG modelContainer = (RenderableSVG) projectifiableThing;
            topLevelThings.add(modelContainer);
            projectModified();
//            fireWhenModelAdded(modelContainer);
//            addModelListeners(modelContainer);
//            for (ModelContainer childModelContainer : modelContainer.getChildModelContainers())
//            {
//                addModelListeners(childModelContainer);
//            }
        }
    }

    @Override
    public void removeModels(Set<ProjectifiableThing> projectifiableThings)
    {
        Set<RenderableSVG> modelContainers = (Set) projectifiableThings;

        for (RenderableSVG modelContainer : modelContainers)
        {
            assert modelContainer != null;
        }

        topLevelThings.removeAll(modelContainers);

//        for (RenderableSVG modelContainer : modelContainers)
//        {
//            removeModelListeners(modelContainer);
//            for (RenderableSVG childModelContainer : modelContainer.getChildModelContainers())
//            {
//                removeModelListeners(childModelContainer);
//            }
//        }
        projectModified();
//        fireWhenModelsRemoved(projectifiableThings);
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
    }

    @Override
    protected void fireWhenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings)
    {
    }

    @Override
    protected void load(ProjectFile projectFile, String basePath) throws ProjectLoadException
    {
        suppressProjectChanged = true;

        if (projectFile instanceof SVGProjectFile)
        {
            try
            {
                projectNameProperty.set(projectFile.getProjectName());
                lastModifiedDate.set(projectFile.getLastModifiedDate());
                lastPrintJobID = projectFile.getLastPrintJobID();

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
            RenderableSVG modelContainer = (RenderableSVG) modelsInput.readObject();
            addModel(modelContainer);
        }
    }
}
