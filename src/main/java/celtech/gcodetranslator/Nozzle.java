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
    private double allowedTravelBeforeClose = 3;
    
    private double currentPosition = 0;
    private double ejectionVolume = 0;
    private double wipeVolume = 0;    
    private double partialBMinimum = 0;

    /**
     *
     * @param nozzleReferenceNumber
     * @param ejectionVolume
     * @param wipeVolume
     * @param partialBMinimum
     */
    public Nozzle(int nozzleReferenceNumber, double ejectionVolume, double wipeVolume, double partialBMinimum)
    {
        this.nozzleReferenceNumber = nozzleReferenceNumber;
        this.ejectionVolume = ejectionVolume;
        this.wipeVolume = wipeVolume;
        this.partialBMinimum = partialBMinimum;
    }

    /**
     *
     * @return
     */
    public NozzleState getState()
    {
        return state;
    }
    
    /**
     *
     * @return
     */
    public int getReferenceNumber()
    {
        return nozzleReferenceNumber;
    }
    
    /**
     *
     * @return
     */
    public double getEjectionVolume()
    {
        return ejectionVolume;
    }
    
    /**
     *
     * @return
     */
    public double getWipeVolume()
    {
        return wipeVolume;
    }
        
    /**
     *
     * @return
     */
    public double getPartialBMinimum()
    {
        return partialBMinimum;
    }

    /**
     *
     * @return
     */
    public double closeNozzleFully()
    {
        currentPosition = closedPosition;
        state = NozzleState.CLOSED;
        return currentPosition;
    }
    
    /**
     *
     * @return
     */
    public double openNozzleFully()
    {
        currentPosition = openPosition;
        state = NozzleState.OPEN;
        return currentPosition;
    }
    
    /**
     *
     * @return
     */
    public double getAllowedTravelBeforeClose()
    {
        return allowedTravelBeforeClose;
    }
}
