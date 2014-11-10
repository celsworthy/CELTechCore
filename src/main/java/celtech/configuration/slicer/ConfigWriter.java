package celtech.configuration.slicer;

import celtech.configuration.fileRepresentation.SlicerParametersFile;

/**
 *
 * @author Ian
 */
public interface ConfigWriter
{
    public void generateConfigForSlicer(SlicerParametersFile profileData, String destinationFile);
    
    public void setPrintCentre(double x, double y);
}
