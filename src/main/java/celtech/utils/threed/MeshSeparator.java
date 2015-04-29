/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.shape.TriangleMesh;

/**
 * MeshSeparator takes an input {@link javafx.scene.shape.TriangleMesh} and returns multiple TriangleMeshes
 * according to the number of separate objects (non-joined) in the input mesh. The concept of
 * 'joining' is based on having a common vertex.
 *
 * @author tony
 */
public class MeshSeparator
{

    /**
     * Separate the given mesh into multiple meshes according to the number of separate (non-joined)
     * objects in the given mesh.
     */
    static List<TriangleMesh> separate(TriangleMesh mesh)
    {
        List<TriangleMesh> meshes = new ArrayList<>();
        meshes.add(mesh);
        return meshes;
    }
    
}
