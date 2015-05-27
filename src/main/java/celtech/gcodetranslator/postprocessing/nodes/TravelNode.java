package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class TravelNode extends MovementNode
{
    //Travel events should always use G1

    @Override
    public String renderForOutput()
    {
        StringBuilder stringToReturn = new StringBuilder();
        
        stringToReturn.append("G1 ");
        
        stringToReturn.append(super.renderForOutput());
        
        return stringToReturn.toString();
    }
}
