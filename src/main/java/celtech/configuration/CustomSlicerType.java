package celtech.configuration;

/**
 *
 * @author Ian
 */
public enum CustomSlicerType
{
    Default(null), Cura(SlicerType.Cura);
    
    private final SlicerType slicerType;

    private CustomSlicerType(SlicerType slicerType)
    {
        this.slicerType = slicerType;
    }
     
    public static CustomSlicerType customTypefromSettings(SlicerType slicerType)
    {
        CustomSlicerType customSlicerType = CustomSlicerType.valueOf(slicerType.name());
        
        return customSlicerType;
    }
    
    public SlicerType getSlicerType()
    {
        return slicerType;
    }
}
