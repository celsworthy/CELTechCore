package celtech.configuration.slicer;

/**
 *
 * @author Ian
 */
public enum FillPattern
{

    LINE("Line"),
    RECTILINEAR("Rectilinear"),
    HONEYCOMB("Honeycomb");

    private String displayText;

    private FillPattern(String displayText)
    {
        this.displayText = displayText;
    }

    @Override
    public String toString()
    {
        return displayText;
    }
}
