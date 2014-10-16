package celtech.configuration.fileRepresentation;

/**
 *
 * @author Ian
 */
public class UserPreferenceFile
{
    private String slicerType = null;
    private boolean overrideSafeties = false;

    public String getSlicerType()
    {
        return slicerType;
    }

    public void setSlicerType(String slicerType)
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
