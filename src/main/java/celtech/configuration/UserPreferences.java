package celtech.configuration;

import celtech.configuration.fileRepresentation.UserPreferenceFile;

/**
 *
 * @author Ian
 */
public class UserPreferences
{

    private SlicerType slicerType = SlicerType.Cura;
    private boolean overrideSafeties = false;

    public UserPreferences(UserPreferenceFile userPreferenceFile)
    {
        String slicerString = userPreferenceFile.getSlicerType();

        if (slicerString != null)
        {
            if (slicerString.equalsIgnoreCase("cura"))
            {
                slicerType = SlicerType.Cura;
            } else if (slicerString.equalsIgnoreCase("slic3r"))
            {
                slicerType = SlicerType.Slic3r;
            }
        } else
        {
            slicerType = SlicerType.Cura;
        }

        this.overrideSafeties = userPreferenceFile.isOverrideSafeties();
    }

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
