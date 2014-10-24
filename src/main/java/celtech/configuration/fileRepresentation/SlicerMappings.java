package celtech.configuration.fileRepresentation;

import celtech.configuration.SlicerType;
import java.util.HashMap;

/**
 *
 * @author Ian
 */
public class SlicerMappings
{

    private HashMap<SlicerType, SlicerMappingData> mappings;

    public HashMap<SlicerType, SlicerMappingData> getMappings()
    {
        return mappings;
    }

    public void setMappings(HashMap<SlicerType, SlicerMappingData> mappings)
    {
        this.mappings = mappings;
    }
}
