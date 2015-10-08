package celtech.gcodetranslator.postprocessing;

/**
 *
 * @author Ian
 */
public class OpenResult
{

    private double outstandingEReplenish;
    private double outstandingDReplenish;
    private final boolean nozzleOpen;

    public OpenResult(double outstandingEReplenish, double outstandingDReplenish, boolean nozzleOpen)
    {
        this.outstandingEReplenish = outstandingEReplenish;
        this.outstandingDReplenish = outstandingDReplenish;
        this.nozzleOpen = nozzleOpen;
    }

    public double getOutstandingDReplenish()
    {
        return outstandingDReplenish;
    }

    public void setOutstandingDReplenish(double outstandingDReplenish)
    {
        this.outstandingDReplenish = outstandingDReplenish;
    }

    public double getOutstandingEReplenish()
    {
        return outstandingEReplenish;
    }

    public void setOutstandingEReplenish(double outstandingEReplenish)
    {
        this.outstandingEReplenish = outstandingEReplenish;
    }

    public boolean isNozzleOpen()
    {
        return nozzleOpen;
    }
}
