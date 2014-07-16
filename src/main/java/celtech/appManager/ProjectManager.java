/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.appManager;

import celtech.configuration.ApplicationConfiguration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProjectManager implements Savable, Serializable
{

    private static ProjectManager instance = null;
    private static List<Project> openProjects = new ArrayList<>();
    private final static String projectFileName = "projects.dat";
    private final static Stenographer steno = StenographerFactory.getStenographer(ProjectManager.class.getName());
    private final static ProjectFileFilter fileFilter = new ProjectFileFilter();

    private ProjectManager()
    {

    }

    /**
     *
     * @return
     */
    public static ProjectManager getInstance()
    {
        if (instance == null)
        {
            ProjectManager pm = loadState();
            if (pm != null)
            {
                instance = pm;
            } else
            {
                instance = new ProjectManager();
            }
        }

        return instance;
    }

    private static ProjectManager loadState()
    {
        File projectDirHandle = new File(ApplicationConfiguration.getProjectDirectory());

        if (!projectDirHandle.exists())
        {
            projectDirHandle.mkdirs();
        }

        ProjectManager pm = null;
        try
        {
            FileInputStream projectFile = new FileInputStream(ApplicationConfiguration.getProjectDirectory() + projectFileName);
            ObjectInputStream reader = new ObjectInputStream(projectFile);
            pm = new ProjectManager();
            int numberOfOpenProjects = reader.readInt();
            for (int counter = 0; counter < numberOfOpenProjects; counter++)
            {
                String projectName = reader.readUTF();
                Project project = loadProject(projectName);
                if (project != null)
                {
                    pm.projectOpened(project);
                }
                else
                {
                    steno.warning("Project Manager tried to load " + projectName + " but it couldn't be opened");
                }
            }
            reader.close();
        } catch (IOException ex)
        {
            steno.error("Failed to load project manager");
        }
        return pm;
    }

    /**
     *
     * @param projectName
     * @return
     */
    public static Project loadProject(String projectName)
    {
        File projectFile = new File(projectName);
        return loadProject(projectFile);
    }

    /**
     *
     * @param projectFile
     * @return
     */
    public static Project loadProject(File projectFile)
    {
        Project loadedProject = null;

        try
        {
            FileInputStream projectFileStream = new FileInputStream(projectFile);
            ObjectInputStream reader = new ObjectInputStream(projectFileStream);
            loadedProject = (Project) reader.readObject();
            reader.close();

            loadedProject.getProjectHeader().setProjectPath(projectFile.getParent());
        } catch (IOException ex)
        {
            ex.printStackTrace();
            steno.error("Failed to load project " + projectFile.getAbsolutePath());
        } catch (ClassNotFoundException ex)
        {
            steno.error("Couldn't locate class while loading project " + projectFile.getAbsolutePath());
        }

        return loadedProject;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean saveState()
    {
        boolean savedSuccessfully = false;

        try
        {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ApplicationConfiguration.getProjectDirectory() + projectFileName));
            out.writeInt(openProjects.size());
            for (Project project : openProjects)
            {
                out.writeUTF(project.getAbsolutePath());
            }
            out.close();
        } catch (FileNotFoundException ex)
        {
            steno.error("Failed to save project state");
        } catch (IOException ex)
        {
            steno.error("Couldn't write project manager state to file");
        }

        return savedSuccessfully;
    }

    /**
     *
     * @param project
     */
    public void projectOpened(Project project)
    {
        if (! openProjects.contains(project)) {
            openProjects.add(project);
        }
    }

    /**
     *
     * @param project
     */
    public void projectClosed(Project project)
    {
        openProjects.remove(project);
    }

    /**
     *
     * @return
     */
    public List<Project> getOpenProjects()
    {
        return openProjects;
    }

    /**
     *
     * @return
     */
    public ObservableList<Project> getAvailableProjects()
    {
        ObservableList<Project> availableProjects = FXCollections.observableArrayList();

        File projectDir = new File(ApplicationConfiguration.getProjectDirectory());
        File[] projectFiles = projectDir.listFiles(fileFilter);
        for (File file : projectFiles)
        {
            try
            {
                FileInputStream projectFile = new FileInputStream(file);
                ObjectInputStream reader = new ObjectInputStream(projectFile);
                Project project = (Project) reader.readObject();
                availableProjects.add(project);
                reader.close();
            } catch (IOException ex)
            {
                steno.error("Failed to load project manager");
            } catch (ClassNotFoundException ex)
            {
                steno.error("Failure whilst loading available project headers");
            }
        }
        return availableProjects;
    }
    
    public Set<String> getOpenAndAvailableProjectNames() {
        Set<String> openAndAvailableProjectNames = new HashSet<>();
        for (Project project : openProjects)
        {
            openAndAvailableProjectNames.add(project.getProjectName());
        }
        for (Project project : getAvailableProjects())
        {
            openAndAvailableProjectNames.add(project.getProjectName());
        }
        return openAndAvailableProjectNames;
    }

}
