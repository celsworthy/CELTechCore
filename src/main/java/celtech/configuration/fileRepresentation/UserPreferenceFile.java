package celtech.configuration.fileRepresentation;

import celtech.configuration.SlicerType;

/**
 *
 * @author Ian
 */
public class UserPreferenceFile
{
    private SlicerType slicerType = null;
    private boolean overrideSafeties = false;

    public SlicerType getSlicerType()
    {
        return slicerType;
    }

    public void setSlicerType(SlicerType slicerType)
    {
        this.slicerType = slicerType;
    }

    public boolean isOverrideSafeties()
    {
        return overrideSafeties;
    }

    public void setOverrideSafeties(boolean overrideSafeties)
    {
        this.overrideSafeties = overrideSafeties;
    }
}
