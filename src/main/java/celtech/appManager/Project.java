package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.PrintBed;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.fileRepresentation.ProjectFile;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.coreUI.visualisation.metaparts.Part;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.utils.Math.Packing.PackingThing;
import celtech.utils.threed.ThreeDUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
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

    private final ObservableList<Part> loadedParts = FXCollections.observableArrayList();
    private String lastPrintJobID = "";
    private final ObjectProperty<Filament> extruder0Filament = new SimpleObjectProperty<>();
    private final ObjectProperty<Filament> extruder1Filament = new SimpleObjectProperty<>();
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
        //TODO sort serialisation
//        ObjectInputStream modelsInput = new ObjectInputStream(
//            new FileInputStream(basePath
//                + ApplicationConfiguration.projectModelsFileExtension));
//        int numModels = modelsInput.readInt();
//        for (int i = 0; i < numModels; i++)
//        {
//            Part part = (Part) modelsInput.readObject();
//            loadedParts.add(modelContainer);
//        }
    }

    public static void saveProject(Project project)
    {
        String basePath = ApplicationConfiguration.getProjectDirectory() + File.separator
            + project.getProjectName();
        project.save(basePath);
    }

    private void saveModels(String path) throws IOException
    {
        //TODO sort serialisation
//        ObjectOutputStream modelsOutput = new ObjectOutputStream(new FileOutputStream(path));
//        modelsOutput.writeInt(loadedParts.size());
//        for (int i = 0; i < loadedParts.size(); i++)
//        {
//            modelsOutput.writeObject(loadedParts.get(i));
//        }
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
        for (Part part : loadedParts)
        {
            int extruderNumber = part.getAssociateWithExtruderNumberProperty().get();
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
//            Part model = (Part) in.readObject();
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
    public ObservableList<Part> getLoadedModels()
    {
        return loadedParts;
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

    public void copyModel(Part modelContainer)
    {
        Part copy = modelContainer.makeCopy();
        addPart(copy);
    }

    public void addPart(Part part)
    {
        loadedParts.add(part);
        projectModified();
        fireWhenModelAdded(part);
        addModelListeners(part);
    }

    private void fireWhenModelAdded(Part part)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelAdded(part);
        }
    }

    public void deleteModel(Part modelContainer)
    {
        loadedParts.remove(modelContainer);
        projectModified();
        fireWhenModelRemoved(modelContainer);
        removeModelListeners(modelContainer);
    }

    private void fireWhenModelRemoved(Part modelContainer)
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

    private void addModelListeners(Part modelContainer)
    {
        modelExtruderNumberListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            fireWhenModelChanged(modelContainer, "associateWithExtruderNumber");
        };
        modelContainer.getAssociateWithExtruderNumberProperty().addListener(
            modelExtruderNumberListener);
    }

    private void removeModelListeners(Part modelContainer)
    {
        modelContainer.getAssociateWithExtruderNumberProperty().removeListener(
            modelExtruderNumberListener);
    }

    public Vector3D getCentreOfLoadedModels()
    {
        return ThreeDUtils.calculateCentre(loadedParts);
    }

    /**
     * ProjectChangesListener allows other objects to observe when models are added or removed etc
     * to the project.
     */
    public interface ProjectChangesListener
    {

        /**
         * This should be fired when a model is added to the project.
         *
         * @param part
         */
        void whenModelAdded(Part part);

        /**
         * This should be fired when a model is removed from the project.
         *
         * @param part
         */
        void whenModelRemoved(Part part);

        /**
         * This should be fired when the project is auto laid out.
         */
        void whenAutoLaidOut();

        /**
         * This should be fired when one or more models have been moved, rotated or scaled etc. If
         * possible try to fire just once for any given group change.
         *
         * @param part
         */
        void whenModelsTransformed(Set<Part> part);

        /**
         * This should be fired when certain details of the model change. Currently this is only: -
         * associatedExtruder
         *
         * @param part
         * @param propertyName
         */
        void whenModelChanged(Part part, String propertyName);
    }

    public void autoLayout()
    {
        Collections.sort(loadedParts);
        PackingThing thing = new PackingThing((int) PrintBed.maxPrintableXSize,
                                              (int) PrintBed.maxPrintableZSize);

        thing.reference(loadedParts, 10);
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

    public void scaleModels(Set<Part> parts, double newScale)
    {
        for (Part model : parts)
        {
            {
                model.setScale(newScale);
            }
        }
        projectModified();
        fireWhenModelsTransformed(parts);
    }

    public void deleteModels(Set<Part> parts)
    {
        for (Part model : parts)
        {
            {
                deleteModel(model);
            }
        }
    }

    public void rotateModels(Set<Part> parts, double rotation)
    {
        for (Part model : parts)
        {
            {
                model.setRotationY(rotation);
            }
        }
        projectModified();
        fireWhenModelsTransformed(parts);
    }

    public void resizeModelsDepth(Set<Part> parts, double depth)
    {
        for (Part model : parts)
        {
            {
                model.resizeDepth(depth);
            }
        }
        projectModified();
        fireWhenModelsTransformed(parts);
    }

    public void resizeModelsHeight(Set<Part> parts, double height)
    {
        for (Part model : parts)
        {
            {
                model.resizeHeight(height);
            }
        }
        projectModified();
        fireWhenModelsTransformed(parts);
    }

    public void resizeModelsWidth(Set<Part> parts, double width)
    {
        for (Part model : parts)
        {
            {
                model.resizeWidth(width);
            }
        }
        projectModified();
        fireWhenModelsTransformed(parts);
    }

    public void cutModelAtHeight(Part model, double heightAboveBed)
    {
        Set<Part> cutModels = model.cutModelAtHeight(heightAboveBed);
        projectModified();
//        fireWhenModelsTransformed(parts);
    }

    private void fireWhenModelsTransformed(Set<Part> parts)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelsTransformed(parts);
        }
    }

    private void fireWhenModelChanged(Part modelContainer, String propertyName)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelChanged(modelContainer, propertyName);
        }
    }

    public void translateModelsXTo(Set<Part> parts, double x)
    {
        for (Part model : parts)
        {
            {
                model.translateXTo(x);
            }
        }
        projectModified();
        fireWhenModelsTransformed(parts);
    }

    public void translateModelsZTo(Set<Part> parts, double z)
    {
        for (Part model : parts)
        {
            {
                model.translateZTo(z);
            }
        }
        projectModified();
        fireWhenModelsTransformed(parts);
    }
}
