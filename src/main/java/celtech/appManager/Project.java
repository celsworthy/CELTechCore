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
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.map.ObjectMapper;

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
    private static final ProjectManager projectManager = ProjectManager.getInstance();

    private ObservableList<ModelContainer> loadedModels = FXCollections.observableArrayList();
    private String lastPrintJobID = "";
    private final ObjectProperty<Filament> extruder0Filament = new SimpleObjectProperty<>();
    private final ObjectProperty<Filament> extruder1Filament = new SimpleObjectProperty<>();
    private PrinterSettings printerSettings;

    private int brimOverride = 0;
    private float fillDensityOverride = 0;
    private boolean printSupportOverride = false;

    private StringProperty projectNameProperty;
    private ObjectProperty<Date> lastModifiedDate = new SimpleObjectProperty<>();

    public Project()
    {
        initialiseExtruderFilaments();
        printerSettings = new PrinterSettings();
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("-hhmmss-ddMMYY");
        projectNameProperty = new SimpleStringProperty(Lookup.i18n("projectLoader.untitled")
            + formatter.format(now));
        lastModifiedDate.set(now);
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

    public void load(String basePath)
    {

        File file = new File(basePath + ApplicationConfiguration.projectFileExtension);

        try
        {
            ProjectFile projectFile = mapper.readValue(file, ProjectFile.class);

            brimOverride = projectFile.getBrimOverride();
            fillDensityOverride = projectFile.getFillDensityOverride();
            printSupportOverride = projectFile.getPrintSupportOverride();
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

            loadModels(basePath);

        } catch (IOException ex)
        {
            steno.error("Failed to load project " + basePath);
        } catch (ClassNotFoundException ex)
        {
            steno.error("Failed to load project " + basePath);
        }
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
            loadedModels.add(modelContainer);
        }
        loadedModels = (ObservableList<ModelContainer>) modelsInput.readObject();
    }

    public static void saveProject(Project project)
    {
        String basePath = ApplicationConfiguration.getProjectDirectory() + File.separator
            + project.getProjectName();
        project.save(basePath);
    }

    private void saveModels(String path) throws IOException
    {
        ObjectOutputStream modelsOutput = new ObjectOutputStream(
            new FileOutputStream(path));
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
            int extruderNumber = loadedModel.getAssociateWithExtruderNumber();
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

    public void projectModified()
    {
        lastPrintJobID = "";
        lastModifiedDate.set(new Date());
    }

    public String getLastPrintJobID()
    {
        return lastPrintJobID;
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
    }

    public void addModelContainer(String fullFilename, ModelContainer modelContainer)
    {
        steno.debug("I am loading " + fullFilename);
        projectManager.projectOpened(this);
        addModel(modelContainer);
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

    public void scaleModels(Set<ModelContainer> modelContainers, double newScale)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.setScale(newScale);
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

    public void rotateModels(Set<ModelContainer> modelContainers, double rotation)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.setRotationY(rotation);
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

    public int getBrimOverride()
    {
        return brimOverride;
    }

    public void setBrimOverride(int brimOverride)
    {
        this.brimOverride = brimOverride;
        projectModified();
    }

    public float getFillDensityOverride()
    {
        return fillDensityOverride;
    }

    public void setFillDensityOverride(float fillDensityOverride)
    {
        this.fillDensityOverride = fillDensityOverride;
        projectModified();
    }

    public boolean getPrintSupportOverride()
    {
        return printSupportOverride;
    }

    public void setPrintSupportOverride(boolean printSupportOverride)
    {
        this.printSupportOverride = printSupportOverride;
        projectModified();
    }
}
