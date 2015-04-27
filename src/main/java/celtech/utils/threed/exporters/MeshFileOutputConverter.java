package celtech.utils.threed.exporters;

import celtech.appManager.Project;

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
     */
    void outputFile(Project project, String printJobUUID);

    /**
     * Output the stl or amf file for the given project to the file indicated by the project job
     * UUID.
     * @param project
     * @param printJobUUID
     * @param printJobDirectory
     */
    void outputFile(Project project, String printJobUUID, String printJobDirectory);
}
