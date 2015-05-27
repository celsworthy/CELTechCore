package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class UnretractNode extends MovementNode
{
    //Uetracts should always use G1

    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 ";
        
        stringToReturn += super.renderForOutput();
        
        return stringToReturn;
    }
}
