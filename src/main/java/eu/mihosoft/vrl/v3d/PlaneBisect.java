package eu.mihosoft.vrl.v3d;

import celtech.roboxbase.utils.TimeUtils;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PlaneBisect
{

    private static Stenographer steno = StenographerFactory.getStenographer(PlaneBisect.class.getName());

    public class TopBottomCutPair
    {

        private CSG topPart;
        private CSG bottomPart;

        public TopBottomCutPair(CSG topPart, CSG bottomPart)
        {
            this.topPart = topPart;
            this.bottomPart = bottomPart;
        }

        public CSG getTopPart()
        {
            return topPart;
        }

        public CSG getBottomPart()
        {
            return bottomPart;
        }
    }

    TimeUtils csgTimer = new TimeUtils();

    public TopBottomCutPair clipAtHeight(CSG modelToClip, double clipHeight)
    {
        steno.info(modelToClip.getBounds().getBounds().toString());

        Node topNode = new Node(modelToClip.getPolygons());
        Node bottomNode = new Node(modelToClip.clone().getPolygons());

        Cube subtractMeToGetTop = new Cube(modelToClip.getBounds().getBounds().x, clipHeight, modelToClip.getBounds().getBounds().z);
        Node subtractMeToGetTopNode = new Node(subtractMeToGetTop.toPolygons());

        csgTimer.timerStart(this, "process");
        topNode.invert();
        topNode.clipTo(subtractMeToGetTopNode);
        subtractMeToGetTopNode.clipTo(topNode);
        subtractMeToGetTopNode.invert();
        subtractMeToGetTopNode.clipTo(topNode);
        subtractMeToGetTopNode.invert();
        topNode.build(subtractMeToGetTopNode.allPolygons());
        topNode.invert();
        csgTimer.timerStop(this, "process");
        steno.info("Time to process " + csgTimer.timeTimeSoFar_ms(this, "process"));

        CSG topModel = CSG.fromPolygons(topNode.allPolygons());
        CSG bottomModel = CSG.fromPolygons(bottomNode.allPolygons());

        TopBottomCutPair result = new TopBottomCutPair(topModel, bottomModel);

        return result;
    }
}
