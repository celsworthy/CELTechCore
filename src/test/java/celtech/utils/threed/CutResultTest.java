/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import java.util.List;
import java.util.Set;
import javafx.scene.shape.TriangleMesh;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author tony
 */
public class CutResultTest
{
    
    @Test
    public void testGetNestedPolygonSets()
    {
        TriangleMesh triangleMesh = new TriangleMesh();
        CutResult cutResult = new CutResult(triangleMesh, null);
        Set<List<List<Integer>>> nestedPolygonSets = cutResult.getNestedPolygonSets();
    }
    
}
