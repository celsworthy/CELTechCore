/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.appManager;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileContainer;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.PrintJob;
import celtech.services.slicer.RoboxProfile;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class Project implements Serializable
{
    
    private static final long serialVersionUID = 1L;
    private ProjectHeader projectHeader = new ProjectHeader();
    private ObservableList<ModelContainer> loadedModels = FXCollections.observableArrayList();
    private String gcodeFileName = "";
    private ObjectProperty<ProjectMode> projectMode = new SimpleObjectProperty<>(ProjectMode.NONE);
    private RoboxProfile customSettings = null;
    private BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private ObservableList<PrintJob> printJobs = FXCollections.observableArrayList();
    
    public Project()
    {
        this.customSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.customSettingsProfileName);
    }
    
    public Project(String preloadedProjectUUID, String projectName, ObservableList<ModelContainer> loadedModels)
    {
        projectHeader.setProjectUUID(preloadedProjectUUID);
        setProjectName(projectName);
        this.loadedModels = loadedModels;
        this.customSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.customSettingsProfileName);
    }
    
    public final void setProjectName(String value)
    {
        projectHeader.setProjectName(value);
    }
    
    public final String getProjectName()
    {
        return projectHeader.getProjectName();
    }
    
    public final StringProperty projectNameProperty()
    {
        return projectHeader.projectNameProperty();
    }
    
    public final String getUUID()
    {
        return projectHeader.getUUID();
    }
    
    public final String getGCodeFilename()
    {
        return gcodeFileName;
    }
    
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
        
        out.writeObject(customSettings);
    }
    
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        projectHeader = (ProjectHeader) in.readObject();
        int numberOfModels = in.readInt();
        loadedModels = FXCollections.observableArrayList();
        for (int counter = 0; counter < numberOfModels; counter++)
        {
            ModelContainer model = (ModelContainer) in.readObject();
            loadedModels.add(model);
        }
        
        gcodeFileName = in.readUTF();

        // We have to be of mesh type as no others are saved...
        projectMode = new SimpleObjectProperty<>(ProjectMode.MESH);
        
        customSettings = (RoboxProfile) in.readObject();
        
        printJobs = FXCollections.observableArrayList();
    }
    
    private void readObjectNoData()
            throws ObjectStreamException
    {
        
    }
    
    public ProjectHeader getProjectHeader()
    {
        return projectHeader;
    }
    
    public ObservableList<ModelContainer> getLoadedModels()
    {
        return loadedModels;
    }
    
    @Override
    public String toString()
    {
        return projectHeader.getProjectName();
    }
    
    public ProjectMode getProjectMode()
    {
        return projectMode.get();
    }
    
    public void setProjectMode(ProjectMode mode)
    {
        projectMode.set(mode);
    }
    
    public ObjectProperty<ProjectMode> projectModeProperty()
    {
        return projectMode;
    }
    
    public void addPrintJob(PrintJob printJob)
    {
        printJobs.add(printJob);
    }
    
    public void projectModified()
    {
        printJobs.clear();
    }
    
    public ObservableList<PrintJob> getPrintJobs()
    {
        return printJobs;
    }
}
