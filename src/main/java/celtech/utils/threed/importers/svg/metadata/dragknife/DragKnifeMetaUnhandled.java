package celtech.utils.threed.importers.svg.metadata.dragknife;

/**
 *
 * @author ianhudson
 */
public class DragKnifeMetaUnhandled extends DragKnifeMetaPart
{
    private final String comment;
    
    public DragKnifeMetaUnhandled(double startX, double startY, double endX, double endY, String comment)
    {
        super(startX, startY, endX, endY, comment);
        this.comment = comment;
    }
    
    public String getComment()
    {
        return comment;
    }
    
    @Override
    public String renderToGCode()
    {
        return "; Unhandled - " + comment;
    }  
}
