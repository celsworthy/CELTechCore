package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class NozzleChangeEvent extends GCodeParseEvent
{
    private int nozzleNumber;

    public int getNozzleNumber()
    {
        return nozzleNumber;
    }

    public void setNozzleNumber(int value)
    {
        this.nozzleNumber = value;
    }

    @Override
    public String renderForOutput()
    {
        return "T" + nozzleNumber + "\n";
    }
}
