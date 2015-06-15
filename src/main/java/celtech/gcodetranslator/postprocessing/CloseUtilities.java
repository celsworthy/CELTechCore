package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.utils.Math.MathUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.apache.commons.math3.geometry.euclidean.twod.Segment;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class CloseUtilities
{

    private final Project project;

    private final float maxDistanceFromEndPoint;
    private final int maxNumberOfIntersectionsToConsider;

    public CloseUtilities(Project project)
    {
        this.project = project;
        maxNumberOfIntersectionsToConsider = project.getPrinterSettings().getSettings().getNumberOfPerimeters();
        maxDistanceFromEndPoint = project.getPrinterSettings().getSettings().getPerimeterExtrusionWidth_mm()
                * 1.01f * maxNumberOfIntersectionsToConsider;
    }

    protected Optional<IntersectionResult> findClosestExtrusionNode(ExtrusionNode node, SectionNode priorSection)
    {
        ExtrusionNode closestNode = null;
        Vector2D intersectionPoint = null;
        Optional<IntersectionResult> result = Optional.empty();

        //If we got here then we need to split this extrusion
        Optional<GCodeEventNode> siblingBefore = node.getSiblingBefore();

        if (!siblingBefore.isPresent())
        {
            throw new RuntimeException("Unable to find prior sibling when looking for inward move");
        }

        if (siblingBefore.get() instanceof ExtrusionNode)
        {
            // We can work out how to split this extrusion
            ExtrusionNode priorExtrusion = (ExtrusionNode) siblingBefore.get();

            //Get an orthogonal to the extrusion we're considering
            Vector2D priorPoint = priorExtrusion.getMovement().toVector2D();
            Vector2D thisPoint = ((MovementProvider) node).getMovement().toVector2D();

            Segment orthogonalSegment = MathUtils.getOrthogonalLineToLinePoints(maxDistanceFromEndPoint, priorPoint, thisPoint);
            Vector2D orthogonalSegmentMidpoint = MathUtils.findMidPoint(orthogonalSegment.getStart(),
                    orthogonalSegment.getEnd());

            List<ExtrusionNode> extrusionNodesUnderConsideration = priorSection.stream()
                    .filter(extrusionnode -> extrusionnode instanceof ExtrusionNode)
                    .map(ExtrusionNode.class::cast)
                    .collect(Collectors.toList());

            Vector2D lastPointConsidered = null;

            double closestDistanceSoFar = 999;

            for (ExtrusionNode extrusionNodeUnderConsideration : extrusionNodesUnderConsideration)
            {
                Vector2D extrusionPoint = extrusionNodeUnderConsideration.getMovement().toVector2D();

                if (lastPointConsidered != null)
                {
                    Segment segmentUnderConsideration = new Segment(lastPointConsidered,
                            extrusionPoint, new Line(
                                    lastPointConsidered,
                                    extrusionPoint, 1e-12));

                    Vector2D tempIntersectionPoint = MathUtils.getSegmentIntersection(
                            orthogonalSegment, segmentUnderConsideration);

                    if (tempIntersectionPoint != null)
                    {
                        double distanceFromMidPoint = tempIntersectionPoint.distance(
                                orthogonalSegmentMidpoint);

                        if (distanceFromMidPoint < closestDistanceSoFar)
                        {
                            closestNode = extrusionNodeUnderConsideration;
                            closestDistanceSoFar = distanceFromMidPoint;
                            intersectionPoint = tempIntersectionPoint;
                        }
                    }
                }

                lastPointConsidered = extrusionPoint;
            }
        } else
        {
            //Default to using the Outer...
            throw new RuntimeException("Error attempting close - have to use outer perimeter " + node.renderForOutput());
        }

        if (closestNode != null
                && intersectionPoint != null)
        {
            result = Optional.of(new IntersectionResult(closestNode, intersectionPoint));
        }

        return result;
    }
}
