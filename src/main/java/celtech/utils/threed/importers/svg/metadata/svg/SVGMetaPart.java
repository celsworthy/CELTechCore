package celtech.utils.threed.importers.svg.metadata.svg;

/**
 *
 * @author ianhudson
 */
public abstract class SVGMetaPart
{
    /**
     *
     * @param currentX
     * @param currentY
     * @return 
     */
    public abstract RenderSVGToDragKnifeMetaResult renderToDragKnifeMetaParts(double currentX, double currentY);
}
