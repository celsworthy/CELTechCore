package celtech.configuration.slicer;

import celtech.configuration.fileRepresentation.SlicerParameters;

/**
 *
 * @author Ian
 */
public interface ConfigWriter
{
    public void generateConfigForSlicer(SlicerParameters profileData, String destinationFile);
    
    public void setPrintCentre(double x, double y);
}
