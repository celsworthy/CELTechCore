package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class ExtrusionNode extends MovementNode
{
    //Extrusion events should always use G1

    @Override
    public String renderForOutput()
    {
        StringBuilder stringToReturn = new StringBuilder();
        
        stringToReturn.append("G1 ");
        
        stringToReturn.append(super.renderForOutput());
        
        return stringToReturn.toString();
    }
    
    public void extrudeUsingEOnly()
    {
        setE(getE() + getD());
        dNotInUse();
    }

    public void extrudeUsingDOnly()
    {
        setD(getE() + getD());
        eNotInUse();
    }
}
