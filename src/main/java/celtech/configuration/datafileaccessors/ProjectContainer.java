package celtech.configuration.datafileaccessors;

import celtech.appManager.ProjectFileFilter;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.ProjectFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 *
 * @author ianhudson
 */
public class ProjectContainer
{
    private static final Stenographer steno = StenographerFactory.getStenographer(ProjectContainer.class.getName());
    private final ObservableList<ProjectFile> completeProjectList = FXCollections.observableArrayList();
    private final ObjectMapper mapper = new ObjectMapper();

    public ProjectContainer()
    {
        ingestProjectFiles();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    }

    public final void ingestProjectFiles()
    {
        File applicationHeadDir = new File(ApplicationConfiguration.getProjectDirectory());
        File[] applicationProjects = applicationHeadDir.listFiles(new ProjectFileFilter());
        ArrayList<ProjectFile> projectList = new ArrayList<>();

        for (File projectFile : applicationProjects)
        {
            try
            {
                ProjectFile projectFileData = mapper.readValue(projectFile, ProjectFile.class);
                projectList.add(projectFileData);

            } catch (IOException ex)
            {
                steno.error("Error loading project " + projectFile.getAbsolutePath());
            }
        }
        completeProjectList.addAll(projectList);

    }

    public ObservableList<ProjectFile> getCompleteProjectList()
    {
        return completeProjectList;
    }

    void saveProjectFiles() throws IOException
    {
        for (ProjectFile projectFile : completeProjectList)
            
        {
            File file = new File(ApplicationConfiguration.getProjectDirectory()
                + projectFile.getProjectName() + ApplicationConfiguration.projectFileExtension);
            mapper.writeValue(file, projectFile);
        }
    }


}
