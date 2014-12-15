package celtech.gcodetranslator;

import celtech.configuration.slicer.NozzleParameters;

/**
 *
 * @author Ian
 */
public class NozzleProxy
{
    private int nozzleReferenceNumber = -1;
    private NozzleParameters nozzleParameters;
    private NozzleState state = NozzleState.CLOSED;
    private double currentPosition = 0;

    NozzleProxy(NozzleParameters nozzleParameters)
    {
        this.nozzleParameters = nozzleParameters;
    }

    /**
     *
     * @return
     */
    public double closeNozzleFully()
    {
        currentPosition = nozzleParameters.getClosedPosition();
        state = NozzleState.CLOSED;
        return currentPosition;
    }

    /**
     *
     * @return
     */
    public double openNozzleFully()
    {
        currentPosition = nozzleParameters.getOpenPosition();
        state = NozzleState.OPEN;
        return currentPosition;
    }

    public int getNozzleReferenceNumber()
    {
        return nozzleReferenceNumber;
    }

    public void setNozzleReferenceNumber(int nozzleReferenceNumber)
    {
        this.nozzleReferenceNumber = nozzleReferenceNumber;
    }

    public NozzleParameters getNozzleParameters()
    {
        return nozzleParameters;
    }

    public void setNozzleParameters(NozzleParameters nozzleParameters)
    {
        this.nozzleParameters = nozzleParameters;
    }

    public NozzleState getState()
    {
        return state;
    }

    public void setState(NozzleState state)
    {
        this.state = state;
    }

    public double getCurrentPosition()
    {
        return currentPosition;
    }

    public void setCurrentPosition(double currentPosition)
    {
        this.currentPosition = currentPosition;
    }

}
