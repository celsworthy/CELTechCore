package celtech.coreUI.visualisation.collision;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.util.ObjectArrayList;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Polygon;
import eu.mihosoft.vrl.v3d.Vertex;
import java.util.List;
import javafx.concurrent.Task;
import javafx.scene.shape.MeshView;
import javax.vecmath.Vector3f;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.fxyz.utils.MeshUtils;

/**
 *
 * @author Ian
 */
public class HullComputer extends Task<CollisionShape>
{

    private final Stenographer steno = StenographerFactory.getStenographer(HullComputer.class.getName());
    private final MeshView meshView;

    public HullComputer(MeshView meshView)
    {
        this.meshView = meshView;
    }

    @Override
    public CollisionShape call() throws Exception
    {
        steno.info("Starting hull computation ");
        CSG modelAsCSG = MeshUtils.mesh2CSG(meshView);
        CSG hull = modelAsCSG.hull();
        List<Polygon> hullPolys = hull.getPolygons();

        ObjectArrayList<Vector3f> vectors = new ObjectArrayList<>();
        for (Polygon poly : hullPolys)
        {
            for (Vertex vert : poly.vertices)
            {
                Vector3f newVector = new Vector3f((float) vert.pos.x, (float) vert.pos.y, (float) vert.pos.z);
                steno.info("Adding vector " + newVector);
                vectors.add(newVector);
            }
        }

        CollisionShape hullShape= new ConvexHullShape(vectors);
        
        steno.info("Finished hull computation");
        return hullShape;
    }
}
