package celtech.utils.threed.exporters;

import celtech.appManager.Project;
import java.util.List;

/**
 *
 * @author Ian
 */
public interface MeshFileOutputConverter
{

    /**
     * Output the stl or amf file for the given project to the file indicated by the project job
     * UUID.
     * @param project
     * @param printJobUUID
     * @param outputAsSingleFile
     * @return List of filenames that have been created
     */
    List<String> outputFile(Project project, String printJobUUID, boolean outputAsSingleFile);

    /**
     * Output the stl or amf file for the given project to the file indicated by the project job
     * UUID.
     * @param project
     * @param printJobUUID
     * @param printJobDirectory
     * @param outputAsSingleFile
     * @return List of filenames that have been created
     */
    List<String> outputFile(Project project, String printJobUUID, String printJobDirectory, boolean outputAsSingleFile);
}
