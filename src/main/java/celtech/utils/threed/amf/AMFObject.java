package celtech.utils.threed.amf;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;

/**
 *
 * @author Ian
 */
public class AMFObject
{

    @JacksonXmlProperty(isAttribute = true)
    private int objectid;

    private Mesh mesh;

    public int getObjectid()
    {
        return objectid;
    }

    public void setObjectid(int objectid)
    {
        this.objectid = objectid;
    }

    public Mesh getMesh()
    {
        return mesh;
    }

    public void setMesh(Mesh mesh)
    {
        this.mesh = mesh;
    }

}
