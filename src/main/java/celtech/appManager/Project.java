package celtech.appManager;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.modelcontrol.ModelContainer;
import celtech.services.slicer.PrintQualityEnumeration;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
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

    private transient Stenographer steno = StenographerFactory.getStenographer(
        Project.class.getName());
    private static final long serialVersionUID = 1L;
    private ProjectHeader projectHeader = new ProjectHeader();
    private ObservableList<ModelContainer> loadedModels = FXCollections.observableArrayList();
    private String gcodeFileName = "";
    private ObjectProperty<ProjectMode> projectMode = new SimpleObjectProperty<>(ProjectMode.NONE);
    private PrintQualityEnumeration printQuality = null;
    private SlicerParametersFile customSettings = null;
    private String customProfileName = "";
    private String lastPrintJobID = "";
    private ObjectProperty<Filament> extruder0Filament = new SimpleObjectProperty<Filament>();
    private ObjectProperty<Filament> extruder1Filament = new SimpleObjectProperty<Filament>();


    /**
     *
     */
    public Project()
    {
        this.customSettings = SlicerParametersContainer.getSettingsByProfileName(
            ApplicationConfiguration.customSettingsProfileName);
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
        this.customSettings = SlicerParametersContainer.getSettingsByProfileName(
            ApplicationConfiguration.customSettingsProfileName);
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

        out.writeObject(customSettings);

        //Introduced in version 1.00.06
        out.writeUTF(customProfileName);
        out.writeObject(printQuality);

        //Introduced in version 1.??
        out.writeUTF(extruder0Filament.get().getFilamentID());
        out.writeUTF(extruder1Filament.get().getFilamentID());
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
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
            customSettings = (SlicerParametersFile) in.readObject();
            //Introduced in version 1.00.06
            if (in.available() > 0)
            {
                customProfileName = in.readUTF();
                printQuality = (PrintQualityEnumeration) in.readObject();
            }
            //Introduced in version 1.??
            if (in.available() > 0)
            {
                extruder0Filament = new SimpleObjectProperty<Filament>();
                extruder1Filament = new SimpleObjectProperty<Filament>();
                String filamentID0 = in.readUTF();
                String filamentID1 = in.readUTF();
                Filament filament0 = FilamentContainer.getFilamentByID(filamentID0);
                Filament filament1 = FilamentContainer.getFilamentByID(filamentID1);
                if (filament0 != null)
                {
                    extruder0Filament.set(filament0);
                }
                if (filament1 != null)
                {
                    extruder1Filament.set(filament1);
                }

            }
        } catch (IOException ex)
        {
            steno.warning("Unable to deserialise settings");
            customSettings = null;
            customProfileName = "";
            printQuality = null;
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
        return printQuality;
    }

    /**
     *
     * @param printQuality
     */
    public void setPrintQuality(PrintQualityEnumeration printQuality)
    {
        if (this.printQuality != printQuality)
        {
            projectModified();
            this.printQuality = printQuality;
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
}
