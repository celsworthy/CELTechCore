package celtech.gcodetranslator;

/**
 *
 * @author Ian
 */
public class Nozzle
{

    private NozzleState state = NozzleState.CLOSED;
    private int nozzleReferenceNumber = -1;
    private double closedPosition = 0;
    private double openPosition = 1;
    private double allowedTravelBeforeClose = 2;
    
    private double currentPosition = 0;
    private double startCloseBy = 0;
    private double finishCloseBy = 0;    

    public Nozzle(int nozzleReferenceNumber, double startCloseBy, double finishCloseBy)
    {
        this.nozzleReferenceNumber = nozzleReferenceNumber;
        this.startCloseBy = startCloseBy;
        this.finishCloseBy = finishCloseBy;
    }

    public NozzleState getState()
    {
        return state;
    }
    
    public int getReferenceNumber()
    {
        return nozzleReferenceNumber;
    }
    
    public double getStartCloseBy()
    {
        return startCloseBy;
    }
    
    public double getFinishCloseBy()
    {
        return finishCloseBy;
    }
    
    public double closeNozzleFully()
    {
        currentPosition = closedPosition;
        state = NozzleState.CLOSED;
        return currentPosition;
    }
    
    public double openNozzleFully()
    {
        currentPosition = openPosition;
        state = NozzleState.OPEN;
        return currentPosition;
    }
    
    public double getAllowedTravelBeforeClose()
    {
        return allowedTravelBeforeClose;
    }
}
