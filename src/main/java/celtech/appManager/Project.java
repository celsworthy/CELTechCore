package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class Project implements Serializable
{

    private static final Filament DEFAULT_FILAMENT = FilamentContainer.getFilamentByID("RBX-ABS-GR499");
    private transient Stenographer steno = StenographerFactory.getStenographer(
        Project.class.getName());
    private static final long serialVersionUID = 1L;
    private ProjectHeader projectHeader = new ProjectHeader();
    private ObservableList<ModelContainer> loadedModels = FXCollections.observableArrayList();
    private String gcodeFileName = "";
    private ObjectProperty<ProjectMode> projectMode = new SimpleObjectProperty<>(ProjectMode.NONE);
    private String customProfileName = "";
    private String lastPrintJobID = "";
    private ObjectProperty<Filament> extruder0Filament = new SimpleObjectProperty<Filament>();
    private ObjectProperty<Filament> extruder1Filament = new SimpleObjectProperty<Filament>();
    private PrinterSettings printerSettings;

    /**
     *
     */
    public Project()
    {
        initialiseExtruderFilaments();
        this.printerSettings = new PrinterSettings();
    }

    /**
     *
     * @param preloadedProjectUUID
     * @param projectName
     * @param loadedModels
     */
    public Project(String preloadedProjectUUID, String projectName,
        ObservableList<ModelContainer> loadedModels)
    {
        projectHeader.setProjectUUID(preloadedProjectUUID);
        setProjectName(projectName);
        this.loadedModels = loadedModels;
        this.printerSettings = new PrinterSettings();
    }

    /**
     *
     * @param value
     */
    public final void setProjectName(String value)
    {
        projectHeader.setProjectName(value);
    }

    /**
     *
     * @return
     */
    public final String getProjectName()
    {
        return projectHeader.getProjectName();
    }

    /**
     *
     * @return
     */
    public final StringProperty projectNameProperty()
    {
        return projectHeader.projectNameProperty();
    }

    /**
     *
     * @return
     */
    public final String getAbsolutePath()
    {
        return projectHeader.getProjectPath() + File.separator + projectHeader.getProjectName()
            + ApplicationConfiguration.projectFileExtension;
    }

    /**
     *
     * @return
     */
    public final String getUUID()
    {
        return projectHeader.getUUID();
    }

    /**
     *
     * @return
     */
    public final String getGCodeFilename()
    {
        return gcodeFileName;
    }

    /**
     *
     * @param gcodeFilename
     */
    public final void setGCodeFilename(String gcodeFilename)
    {
        this.gcodeFileName = gcodeFilename;
    }

    private void writeObject(ObjectOutputStream out)
        throws IOException
    {
        out.writeObject(projectHeader);
        out.writeInt(loadedModels.size());
        for (ModelContainer model : loadedModels)
        {
            out.writeObject(model);
        }
        out.writeUTF(gcodeFileName);
        out.writeUTF(lastPrintJobID);

        //Introduced in version 1.00.06
        out.writeUTF(customProfileName);

        //Introduced in version 1.??
        if (extruder0Filament.get() != null)
        {
            out.writeUTF(extruder0Filament.get().getFilamentID());
        } else
        {
            out.writeUTF("NULL");
        }
        if (extruder1Filament.get() != null)
        {
            out.writeUTF(extruder1Filament.get().getFilamentID());
        } else
        {
            out.writeUTF("NULL");
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
     * @return 
     */
    public Set<Integer> getUsedExtruders() {
        Set<Integer> usedExtruders = new HashSet<>();
        for (ModelContainer loadedModel : loadedModels)
        {
            int extruderNumber = loadedModel.getAssociateWithExtruderNumber();
            if (! usedExtruders.contains(extruderNumber))
            {
                usedExtruders.add(extruderNumber);
            }
        }
        return usedExtruders;
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        
        printerSettings = new PrinterSettings();
        extruder0Filament = new SimpleObjectProperty<Filament>();
        extruder1Filament = new SimpleObjectProperty<Filament>();
        
        steno = StenographerFactory.getStenographer(Project.class.getName());

        projectHeader = (ProjectHeader) in.readObject();
        int numberOfModels = in.readInt();
        loadedModels = FXCollections.observableArrayList();
        for (int counter = 0; counter < numberOfModels; counter++)
        {
            ModelContainer model = (ModelContainer) in.readObject();
            loadedModels.add(model);
        }

        gcodeFileName = in.readUTF();
        lastPrintJobID = in.readUTF();

        // We have to be of mesh type as no others are saved...
        projectMode = new SimpleObjectProperty<>(ProjectMode.MESH);

        try
        {
//            SlicerParametersFile settings = (SlicerParametersFile) in.readObject();
            //Introduced in version 1.00.06
            if (in.available() > 0)
            {
                customProfileName = in.readUTF();
            }
            //Introduced in version 1.??
            if (in.available() > 0)
            {
                extruder0Filament = new SimpleObjectProperty<Filament>();
                extruder1Filament = new SimpleObjectProperty<Filament>();
                String filamentID0 = in.readUTF();
                String filamentID1 = in.readUTF();
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

            }
        } catch (IOException ex)
        {
            steno.warning("Unable to deserialise settings " + ex);
            customProfileName = "";
        }

    }

    private void readObjectNoData()
        throws ObjectStreamException
    {

    }

    /**
     *
     * @return
     */
    public ProjectHeader getProjectHeader()
    {
        return projectHeader;
    }

    /**
     *
     * @return
     */
    public ObservableList<ModelContainer> getLoadedModels()
    {
        return loadedModels;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return projectHeader.getProjectName();
    }

    /**
     *
     * @return
     */
    public ProjectMode getProjectMode()
    {
        return projectMode.get();
    }

    /**
     *
     * @param mode
     */
    public void setProjectMode(ProjectMode mode)
    {
        projectMode.set(mode);
    }

    /**
     *
     * @return
     */
    public ObjectProperty<ProjectMode> projectModeProperty()
    {
        return projectMode;
    }

    /**
     *
     * @param printJobID
     */
    public void addPrintJobID(String printJobID)
    {
        lastPrintJobID = printJobID;
    }

    /**
     *
     */
    public void projectModified()
    {
        lastPrintJobID = "";
    }

    /**
     *
     * @return
     */
    public String getLastPrintJobID()
    {
        return lastPrintJobID;
    }

    /**
     *
     * @return
     */
    public PrintQualityEnumeration getPrintQuality()
    {
        return printerSettings.getPrintQuality();
    }

    /**
     *
     * @param printQuality
     */
    public void setPrintQuality(PrintQualityEnumeration printQuality)
    {
        if (printerSettings.getPrintQuality() != printQuality)
        {
            projectModified();
            printerSettings.setPrintQuality(printQuality);
        }
    }

    /**
     *
     * @return
     */
    public String getCustomProfileName()
    {
        return customProfileName;
    }

    /**
     *
     * @param customProfileName
     */
    public void setCustomProfileName(String customProfileName)
    {
        if (customProfileName == null)
        {
            this.customProfileName = "";
        } else if (this.customProfileName.equals(customProfileName) == false)
        {
            projectModified();
            this.customProfileName = customProfileName;
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
        // defaults in case of no printer or reel
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
}
