package celtech.utils.threed.importers.svg.metadata.dragknife;

/**
 *
 * @author ianhudson
 */
public class DragKnifeMetaCut extends DragKnifeMetaPart
{

    public DragKnifeMetaCut(double startX, double startY, double endX, double endY, String comment)
    {
        super(startX, startY, endX, endY, comment);
    }

    @Override
    public String renderToGCode()
    {
        String gcodeLine = generateXYMove(getEnd().getX(), getEnd().getY(), "Cut " + getComment());
        return gcodeLine;
    }
}
