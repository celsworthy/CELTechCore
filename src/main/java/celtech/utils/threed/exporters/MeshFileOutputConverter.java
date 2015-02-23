package celtech.utils.threed.exporters;

import celtech.appManager.Project;

/**
 *
 * @author Ian
 */
public interface MeshFileOutputConverter
{
    /**
     *
     * @param project
     * @param printJobUUID
     */
    void outputFile(Project project, String printJobUUID);
}
