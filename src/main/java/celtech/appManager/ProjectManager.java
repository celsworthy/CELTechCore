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
    private final static Stenographer steno = StenographerFactory.getStenographer(
        ProjectManager.class.getName());
    private final static ProjectFileFilter fileFilter = new ProjectFileFilter();

    private ProjectManager()
    {
    }

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
            FileInputStream projectFile = new FileInputStream(
                ApplicationConfiguration.getProjectDirectory() + projectFileName);
            ObjectInputStream reader = new ObjectInputStream(projectFile);
            pm = new ProjectManager();
            int numberOfOpenProjects = reader.readInt();
            for (int counter = 0; counter < numberOfOpenProjects; counter++)
            {
                String projectPath = reader.readUTF();
                Project project = loadProject(projectPath);
                if (project != null)
                {
                    pm.projectOpened(project);
                } else
                {
                    steno.warning("Project Manager tried to load " + projectPath
                        + " but it couldn't be opened");
                }
            }
            reader.close();
        } catch (IOException ex)
        {
            steno.error("Failed to load project manager");
        }
        return pm;
    }

    public static Project loadProject(String projectPath)
    {
        String basePath = projectPath.substring(0, projectPath.lastIndexOf('.'));
        return Project.loadProject(basePath);
    }

    @Override
    public boolean saveState()
    {
        boolean savedSuccessfully = false;

        try
        {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
                ApplicationConfiguration.getProjectDirectory() + projectFileName));
            out.writeInt(openProjects.size());
            for (Project project : openProjects)
            {
                if (project.getLoadedModels().size() > 0)
                {
                    out.writeUTF(project.getAbsolutePath());
                }
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

    public void projectOpened(Project project)
    {
        if (!openProjects.contains(project))
        {
            openProjects.add(project);
        }
    }

    public void projectClosed(Project project)
    {
        openProjects.remove(project);
    }

    public List<Project> getOpenProjects()
    {
        return openProjects;
    }

    public Set<String> getAvailableProjectNames()
    {
        Set<String> availableProjectNames = new HashSet<>();

        File projectDir = new File(ApplicationConfiguration.getProjectDirectory());
        File[] projectFiles = projectDir.listFiles(fileFilter);
        for (File file : projectFiles)
        {
            String[] fileNameElements = file.getAbsolutePath().split(File.separator);
            String fileName = fileNameElements[fileNameElements.length - 1];
            String projectName = fileName.substring(0, fileName.length() - 6);
            availableProjectNames.add(projectName);
        }
        System.out.println("available names are " + availableProjectNames);
        return availableProjectNames;
    }

    public Set<String> getOpenAndAvailableProjectNames()
    {
        Set<String> openAndAvailableProjectNames = new HashSet<>();
        for (Project project : openProjects)
        {
            openAndAvailableProjectNames.add(project.getProjectName());
        }
        openAndAvailableProjectNames.addAll(getAvailableProjectNames());
        return openAndAvailableProjectNames;
    }

}
