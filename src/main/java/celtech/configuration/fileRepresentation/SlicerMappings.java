package celtech.configuration.fileRepresentation;

import celtech.configuration.SlicerType;
import java.util.HashMap;

/**
 *
 * @author Ian
 */
public class SlicerMappings
{

    private HashMap<SlicerType, HashMap<String, String>> mappings;

    public HashMap<SlicerType, HashMap<String, String>> getMappings()
    {
        return mappings;
    }

    public void setMappings(HashMap<SlicerType, HashMap<String, String>> mappings)
    {
        this.mappings = mappings;
    }

    public HashMap<String, String> getMappingData(SlicerType slicerType)
    {
        return this.mappings.get(slicerType);
    }

    public void setMappingData(SlicerType slicerType, HashMap<String, String> mappingData)
    {
        this.mappings.put(slicerType, mappingData);
    }
}
