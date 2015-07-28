package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.utils.Math.MathUtils;
import java.util.Iterator;
import java.util.Optional;
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

    public CloseUtilities(Project project, SlicerParametersFile settings, String headType)
    {
        this.project = project;
        maxNumberOfIntersectionsToConsider = settings.getNumberOfPerimeters();
        maxDistanceFromEndPoint = settings.getPerimeterExtrusionWidth_mm()
                * 1.01f * maxNumberOfIntersectionsToConsider;
    }

    protected Optional<IntersectionResult> findClosestMovementNode(ExtrusionNode node, SectionNode priorSection)
    {
        GCodeEventNode closestNode = null;
        Vector2D intersectionPoint = null;
        Optional<IntersectionResult> result = Optional.empty();

        //If we got here then we need to split this extrusion
        Optional<GCodeEventNode> siblingBefore = node.getSiblingBefore();

        if (!siblingBefore.isPresent())
        {
            throw new RuntimeException("Unable to find prior sibling when looking for inward move");
        }

        if (siblingBefore.get() instanceof MovementProvider)
        {
            // We can work out how to split this extrusion
            Movement priorMovement = ((MovementProvider) siblingBefore.get()).getMovement();

            //Get an orthogonal to the extrusion we're considering
            Vector2D priorPoint = priorMovement.toVector2D();
            Vector2D thisPoint = ((MovementProvider) node).getMovement().toVector2D();

            // We want the orthogonal line to be closer to the specified end point rather than the prior point
            Vector2D vectorFromPriorToThis = thisPoint.subtract(priorPoint);
            Vector2D halfwayBetweenPriorAndThisPoint = priorPoint.add(vectorFromPriorToThis.scalarMultiply(0.5));

            Segment orthogonalSegment = MathUtils.getOrthogonalLineToLinePoints(maxDistanceFromEndPoint, halfwayBetweenPriorAndThisPoint, thisPoint);
            Vector2D orthogonalSegmentMidpoint = MathUtils.findMidPoint(orthogonalSegment.getStart(),
                    orthogonalSegment.getEnd());

            GCodeEventNode lastNodeConsidered = null;

            double closestDistanceSoFar = 999;

            Iterator<GCodeEventNode> priorSectionTreeIterator = priorSection.treeSpanningIterator(null);

            while (priorSectionTreeIterator.hasNext())
            {
                GCodeEventNode priorSectionChild = priorSectionTreeIterator.next();

                if (priorSectionChild instanceof MovementProvider)
                {
                    MovementProvider movementProvider = (MovementProvider) priorSectionChild;
                    Vector2D extrusionPoint = movementProvider.getMovement().toVector2D();

                    if (lastNodeConsidered != null)
                    {
                        Vector2D lastPoint = ((MovementProvider) lastNodeConsidered).getMovement().toVector2D();
                        Segment segmentUnderConsideration = new Segment(lastPoint,
                                extrusionPoint,
                                new Line(lastPoint, extrusionPoint, 1e-12));

                        Vector2D tempIntersectionPoint = MathUtils.getSegmentIntersection(
                                orthogonalSegment, segmentUnderConsideration);

                        if (tempIntersectionPoint != null)
                        {
                            double distanceFromMidPoint = tempIntersectionPoint.distance(
                                    orthogonalSegmentMidpoint);

                            if (distanceFromMidPoint < closestDistanceSoFar)
                            {
                                //Which node was closest - the last one or this one?
                                if (tempIntersectionPoint.distance(lastPoint)
                                        < tempIntersectionPoint.distance(extrusionPoint))
                                {
                                    closestNode = lastNodeConsidered;
                                } else
                                {
                                    closestNode = priorSectionChild;
                                }
                                closestDistanceSoFar = distanceFromMidPoint;
                                intersectionPoint = tempIntersectionPoint;
                            }
                        }
                    }

                    lastNodeConsidered = priorSectionChild;
                }
            }
        } else
        {
            throw new RuntimeException("Prior sibling was not a movement provider " + node.renderForOutput());
        }

        if (closestNode != null
                && intersectionPoint != null)
        {
            result = Optional.of(new IntersectionResult(closestNode, intersectionPoint));
        }

        return result;
    }
}
