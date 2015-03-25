package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.PrintBed;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.fileRepresentation.ProjectFile;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.utils.Math.Packing.PackingThing;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class Project implements Serializable
{

    private static final Filament DEFAULT_FILAMENT = FilamentContainer.getFilamentByID(
        "RBX-ABS-GR499");

    private static final Stenographer steno = StenographerFactory.getStenographer(
        Project.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ObservableList<ModelContainer> loadedModels = FXCollections.observableArrayList();
    private String lastPrintJobID = "";
    private final ObjectProperty<Filament> extruder0Filament = new SimpleObjectProperty<>();
    private final ObjectProperty<Filament> extruder1Filament = new SimpleObjectProperty<>();
    private final BooleanProperty modelColourChanged = new SimpleBooleanProperty();

    private final PrinterSettings printerSettings;

    private final StringProperty projectNameProperty;
    private final ObjectProperty<Date> lastModifiedDate = new SimpleObjectProperty<>();

    private boolean suppressProjectChanged = false;

    public Project()
    {
        initialiseExtruderFilaments();
        printerSettings = new PrinterSettings();
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("-hhmmss-ddMMYY");
        projectNameProperty = new SimpleStringProperty(Lookup.i18n("projectLoader.untitled")
            + formatter.format(now));
        lastModifiedDate.set(now);
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        printerSettings.getDataChanged().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                projectModified();
            });
    }

    public final void setProjectName(String value)
    {
        projectNameProperty.set(value);
    }

    public final String getProjectName()
    {
        return projectNameProperty.get();
    }

    public final StringProperty projectNameProperty()
    {
        return projectNameProperty;
    }

    public final String getAbsolutePath()
    {
        return ApplicationConfiguration.getProjectDirectory() + File.separator
            + projectNameProperty.get()
            + ApplicationConfiguration.projectFileExtension;
    }

    static Project loadProject(String basePath)
    {
        Project project = new Project();
        project.load(basePath);
        return project;
    }

    private void load(String basePath)
    {
        suppressProjectChanged = true;

        File file = new File(basePath + ApplicationConfiguration.projectFileExtension);

        try
        {
            ProjectFile projectFile = mapper.readValue(file, ProjectFile.class);

            projectNameProperty.set(projectFile.getProjectName());
            lastModifiedDate.set(projectFile.getLastModifiedDate());
            lastPrintJobID = projectFile.getLastPrintJobID();

            String filamentID0 = projectFile.getExtruder0FilamentID();
            String filamentID1 = projectFile.getExtruder1FilamentID();
            if (!filamentID0.equals("NULL"))
            {
                Filament filament0 = FilamentContainer.getFilamentByID(filamentID0);
                if (filament0 != null)
                {
                    extruder0Filament.set(filament0);
                }
            }
            if (!filamentID1.equals("NULL"))
            {
                Filament filament1 = FilamentContainer.getFilamentByID(filamentID1);
                if (filament1 != null)
                {
                    extruder1Filament.set(filament1);
                }
            }

            printerSettings.setSettingsName(projectFile.getSettingsName());
            printerSettings.setPrintQuality(projectFile.getPrintQuality());
            printerSettings.setBrimOverride(projectFile.getBrimOverride());
            printerSettings.setFillDensityOverride(projectFile.getFillDensityOverride());
            printerSettings.setPrintSupportOverride(projectFile.getPrintSupportOverride());

            loadModels(basePath);

        } catch (IOException ex)
        {
            steno.error("Failed to load project " + basePath);
        } catch (ClassNotFoundException ex)
        {
            steno.error("Failed to load project " + basePath);
        }
        suppressProjectChanged = false;
    }

    private void loadModels(String basePath) throws IOException, ClassNotFoundException
    {
        ObjectInputStream modelsInput = new ObjectInputStream(
            new FileInputStream(basePath
                + ApplicationConfiguration.projectModelsFileExtension));
        int numModels = modelsInput.readInt();
        for (int i = 0; i < numModels; i++)
        {
            ModelContainer modelContainer = (ModelContainer) modelsInput.readObject();
            addModel(modelContainer);
        }
    }

    public static void saveProject(Project project)
    {
        String basePath = ApplicationConfiguration.getProjectDirectory() + File.separator
            + project.getProjectName();
        project.save(basePath);
    }

    private void saveModels(String path) throws IOException
    {
        ObjectOutputStream modelsOutput = new ObjectOutputStream(new FileOutputStream(path));
        modelsOutput.writeInt(loadedModels.size());
        for (int i = 0; i < loadedModels.size(); i++)
        {
            modelsOutput.writeObject(loadedModels.get(i));
        }
    }

    private void save(String basePath)
    {
        if (getLoadedModels().size() > 0)
        {
            try
            {
                ProjectFile projectFile = new ProjectFile();
                projectFile.populateFromProject(this);
                File file = new File(basePath + ApplicationConfiguration.projectFileExtension);
                mapper.writeValue(file, projectFile);
                saveModels(basePath + ApplicationConfiguration.projectModelsFileExtension);
            } catch (FileNotFoundException ex)
            {
                steno.error("Failed to save project state");
            } catch (IOException ex)
            {
                steno.error(
                    "Couldn't write project state to file for project "
                    + projectNameProperty.get());
            }
        }
    }

    /**
     * Return true if all objects are on the same extruder, else return false.
     */
    public boolean allModelsOnSameExtruder()
    {
        return getUsedExtruders().size() < 2;
    }

    /**
     * Return which extruders are used by the project, as a set of the extruder numbers
     *
     * @return
     */
    public Set<Integer> getUsedExtruders()
    {
        Set<Integer> usedExtruders = new HashSet<>();
        for (ModelContainer loadedModel : loadedModels)
        {
            int extruderNumber = loadedModel.getAssociateWithExtruderNumberProperty().get();
            if (!usedExtruders.contains(extruderNumber))
            {
                usedExtruders.add(extruderNumber);
            }
        }
        return usedExtruders;
    }

//    private void readObject(ObjectInputStream in)
//        throws IOException, ClassNotFoundException
//    {
//
//        printerSettings = new PrinterSettings();
//        extruder0Filament = new SimpleObjectProperty<>();
//        extruder1Filament = new SimpleObjectProperty<>();
//        projectChangesListeners = new HashSet<>();
//
//        steno = StenographerFactory.getStenographer(Project.class.getName());
//
//        projectHeader = (ProjectHeader) in.readObject();
//        int numberOfModels = in.readInt();
//        loadedModels = FXCollections.observableArrayList();
//        for (int counter = 0; counter < numberOfModels; counter++)
//        {
//            ModelContainer model = (ModelContainer) in.readObject();
//            loadedModels.add(model);
//        }
//
//        gcodeFileName = in.readUTF();
//        lastPrintJobID = in.readUTF();
//
//        // We have to be of mesh type as no others are saved...
//        projectMode = new SimpleObjectProperty<>(ProjectMode.MESH);
//
//        try
//        {
////            SlicerParametersFile settings = (SlicerParametersFile) in.readObject();
////            //Introduced in version 1.00.06
////            if (in.available() > 0)
////            {
////                customProfileName = in.readUTF();
////            }
//            //Introduced in version 1.??
//            if (in.available() > 0)
//            {
//                extruder0Filament = new SimpleObjectProperty<Filament>();
//                extruder1Filament = new SimpleObjectProperty<Filament>();
//                String filamentID0 = in.readUTF();
//                String filamentID1 = in.readUTF();
//                if (!filamentID0.equals("NULL"))
//                {
//                    Filament filament0 = FilamentContainer.getFilamentByID(filamentID0);
//                    if (filament0 != null)
//                    {
//                        extruder0Filament.set(filament0);
//                    }
//                }
//                if (!filamentID1.equals("NULL"))
//                {
//                    Filament filament1 = FilamentContainer.getFilamentByID(filamentID1);
//                    if (filament1 != null)
//                    {
//                        extruder1Filament.set(filament1);
//                    }
//                }
//
//            }
//        } catch (IOException ex)
//        {
//            steno.warning("Unable to deserialise settings " + ex);
////            customProfileName = "";
//        }
//
//    }
//    private void readObjectNoData()
//        throws ObjectStreamException
//    {
//
//    }
    public ObservableList<ModelContainer> getLoadedModels()
    {
        return loadedModels;
    }

    @Override
    public String toString()
    {
        return projectNameProperty.get();
    }

    public void setLastPrintJobID(String printJobID)
    {
        lastPrintJobID = printJobID;
    }

    public final void projectModified()
    {
        if (!suppressProjectChanged)
        {
            lastPrintJobID = "";
            lastModifiedDate.set(new Date());
        }
    }

    public String getLastPrintJobID()
    {
        return lastPrintJobID;
    }

    public ReadOnlyBooleanProperty getModelColourChanged()
    {
        return modelColourChanged;
    }

    public PrintQualityEnumeration getPrintQuality()
    {
        return printerSettings.getPrintQuality();
    }

    public void setPrintQuality(PrintQualityEnumeration printQuality)
    {
        if (printerSettings.getPrintQuality() != printQuality)
        {
            projectModified();
            printerSettings.setPrintQuality(printQuality);
        }
    }

    public void setExtruder0Filament(Filament filament)
    {
        extruder0Filament.set(filament);
    }

    public void setExtruder1Filament(Filament filament)
    {
        extruder1Filament.set(filament);
    }

    public ObjectProperty<Filament> getExtruder0FilamentProperty()
    {
        return extruder0Filament;
    }

    public ObjectProperty<Filament> getExtruder1FilamentProperty()
    {
        return extruder1Filament;
    }

    /**
     * For new projects this should be called to initialise the extruder filaments according to the
     * currently selected printer.
     */
    private void initialiseExtruderFilaments()
    {
        // set defaults in case of no printer or reel
        extruder0Filament.set(DEFAULT_FILAMENT);
        extruder1Filament.set(DEFAULT_FILAMENT);

        Printer printer = Lookup.getCurrentlySelectedPrinterProperty().get();
        if (printer != null)
        {
            if (printer.reelsProperty().containsKey(0))
            {
                String filamentID = printer.reelsProperty().get(0).filamentIDProperty().get();
                extruder0Filament.set(FilamentContainer.getFilamentByID(filamentID));
            }
            if (printer.reelsProperty().containsKey(1))
            {
                String filamentID = printer.reelsProperty().get(1).filamentIDProperty().get();
                extruder1Filament.set(FilamentContainer.getFilamentByID(filamentID));
            }
        }
    }

    public PrinterSettings getPrinterSettings()
    {
        return printerSettings;
    }

    public void copyModel(ModelContainer modelContainer)
    {
        ModelContainer copy = modelContainer.makeCopy();
        addModel(copy);
    }

    public void addModel(ModelContainer modelContainer)
    {
        loadedModels.add(modelContainer);
        projectModified();
        fireWhenModelAdded(modelContainer);
        addModelListeners(modelContainer);
    }

    private void fireWhenModelAdded(ModelContainer modelContainer)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelAdded(modelContainer);
        }
    }

    public void deleteModel(ModelContainer modelContainer)
    {
        loadedModels.remove(modelContainer);
        projectModified();
        fireWhenModelRemoved(modelContainer);
        removeModelListeners(modelContainer);
    }

    private void fireWhenModelRemoved(ModelContainer modelContainer)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelRemoved(modelContainer);
        }
    }

    Set<ProjectChangesListener> projectChangesListeners = new HashSet<>();

    public void addProjectChangesListener(ProjectChangesListener projectChangesListener)
    {
        projectChangesListeners.add(projectChangesListener);
    }

    public void removeProjectChangesListener(ProjectChangesListener projectChangesListener)
    {
        projectChangesListeners.remove(projectChangesListener);
    }

    public ObjectProperty<Date> getLastModifiedDate()
    {
        return lastModifiedDate;
    }

    private ChangeListener<Number> modelExtruderNumberListener;

    private void addModelListeners(ModelContainer modelContainer)
    {
        modelExtruderNumberListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            fireWhenModelChanged(modelContainer, "associateWithExtruderNumber");
            modelColourChanged.set(!modelColourChanged.get());
        };
        modelContainer.getAssociateWithExtruderNumberProperty().addListener(
            modelExtruderNumberListener);
    }

    private void removeModelListeners(ModelContainer modelContainer)
    {
        modelContainer.getAssociateWithExtruderNumberProperty().removeListener(
            modelExtruderNumberListener);
    }

    /**
     * ProjectChangesListener allows other objects to observe when models are added or removed etc
     * to the project.
     */
    public interface ProjectChangesListener
    {

        /**
         * This should be fired when a model is added to the project.
         */
        void whenModelAdded(ModelContainer modelContainer);

        /**
         * This should be fired when a model is removed from the project.
         */
        void whenModelRemoved(ModelContainer modelContainer);

        /**
         * This should be fired when the project is auto laid out.
         */
        void whenAutoLaidOut();

        /**
         * This should be fired when one or more models have been moved, rotated or scaled etc. If
         * possible try to fire just once for any given group change.
         */
        void whenModelsTransformed(Set<ModelContainer> modelContainers);

        /**
         * This should be fired when certain details of the model change. Currently this is only: -
         * associatedExtruder
         */
        void whenModelChanged(ModelContainer modelContainer, String propertyName);
    }

    public void autoLayout()
    {
        Collections.sort(loadedModels);
        PackingThing thing = new PackingThing((int) PrintBed.maxPrintableXSize,
                                              (int) PrintBed.maxPrintableZSize);

        thing.reference(loadedModels, 10);
        thing.pack();
        thing.relocateBlocks();

        projectModified();
        fireWhenAutoLaidOut();
    }

    private void fireWhenAutoLaidOut()
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenAutoLaidOut();
        }
    }

    /**
     * Scale X, Y and Z by the given factor, apply the given ratio to the given scale. I.e. the
     * ratio is not an absolute figure to be applied to the models but a ratio to be applied 
     * to the current scale.
     */
    public void scaleXYZRatioSelection(Set<ModelContainer> modelContainers, double ratio)
    {
        for (ModelContainer model : modelContainers)
        {
            model.setXScale(model.getXScale() * ratio);
            model.setYScale(model.getYScale() * ratio);
            model.setZScale(model.getZScale() * ratio);
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void scaleXModels(Set<ModelContainer> modelContainers, double newScale,
        boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert(modelContainers.size() == 1);
            ModelContainer model = modelContainers.iterator().next();
            double ratio = newScale / model.getXScale();
            scaleXYZRatioSelection(modelContainers, ratio);
        } else
        {
            for (ModelContainer model : modelContainers)
            {
                {
                    model.setXScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void scaleYModels(Set<ModelContainer> modelContainers, double newScale,
        boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert(modelContainers.size() == 1);
            ModelContainer model = modelContainers.iterator().next();
            double ratio = newScale / model.getYScale();
            scaleXYZRatioSelection(modelContainers, ratio);
        } else
        {
            for (ModelContainer model : modelContainers)
            {
                {
                    model.setYScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void scaleZModels(Set<ModelContainer> modelContainers, double newScale,
        boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert(modelContainers.size() == 1);
            ModelContainer model = modelContainers.iterator().next();
            double ratio = newScale / model.getZScale();
            scaleXYZRatioSelection(modelContainers, ratio);
        } else
        {
            for (ModelContainer model : modelContainers)
            {
                {
                    model.setZScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void deleteModels(Set<ModelContainer> modelContainers)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                deleteModel(model);
            }
        }
    }

    public void rotateLeanModels(Set<ModelContainer> modelContainers, double rotation)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.setRotationLean(rotation);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void rotateTwistModels(Set<ModelContainer> modelContainers, double rotation)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.setRotationTwist(rotation);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void rotateTurnModels(Set<ModelContainer> modelContainers, double rotation)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.setRotationTurn(rotation);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void resizeModelsDepth(Set<ModelContainer> modelContainers, double depth)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.resizeDepth(depth);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void resizeModelsHeight(Set<ModelContainer> modelContainers, double height)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.resizeHeight(height);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void resizeModelsWidth(Set<ModelContainer> modelContainers, double width)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.resizeWidth(width);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    private void fireWhenModelsTransformed(Set<ModelContainer> modelContainers)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelsTransformed(modelContainers);
        }
    }

    private void fireWhenModelChanged(ModelContainer modelContainer, String propertyName)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelChanged(modelContainer, propertyName);
        }
    }

    public void translateModelsXTo(Set<ModelContainer> modelContainers, double x)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.translateXTo(x);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void translateModelsZTo(Set<ModelContainer> modelContainers, double z)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.translateZTo(z);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }
    
     public Set<ModelContainer.State> getModelStates() {
         Set<ModelContainer.State> modelStates = new HashSet<>();
         for (ModelContainer model : loadedModels)
         {
             modelStates.add(model.getState());
         }
         return modelStates;
     }
     
     public void setModelStates(Set<ModelContainer.State> modelStates) {
         Set<ModelContainer> modelContainers = new HashSet<>();
         for (ModelContainer.State modelState : modelStates)
         {
             for (ModelContainer model : loadedModels)
             {
                 if (model.getModelId() == modelState.modelId) {
                     model.setState(modelState);
                     modelContainers.add(model);
                 }
             }
         }
         projectModified();
         fireWhenModelsTransformed(modelContainers);
     }
}
