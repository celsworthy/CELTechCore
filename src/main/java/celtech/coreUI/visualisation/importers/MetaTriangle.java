/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.coreUI.visualisation.importers;

import javafx.geometry.Point3D;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class MetaTriangle
{

    private Long vertex1Hash = null;
    private Long vertex2Hash = null;
    private Long vertex3Hash = null;

    public MetaTriangle(Long vertex1Hash, Long vertex2Hash, Long vertex3Hash)
    {
        this.vertex1Hash = vertex1Hash;
        this.vertex2Hash = vertex2Hash;
        this.vertex3Hash = vertex3Hash;
    }

    public Long getVertex1Hash()
    {
        return vertex1Hash;
    }

    public void setVertex1Hash(Long vertex1Hash)
    {
        this.vertex1Hash = vertex1Hash;
    }

    public Long getVertex2Hash()
    {
        return vertex2Hash;
    }

    public void setVertex2Hash(Long vertex2Hash)
    {
        this.vertex2Hash = vertex2Hash;
    }

    public Long getVertex3Hash()
    {
        return vertex3Hash;
    }

    public void setVertex3Hash(Long vertex3Hash)
    {
        this.vertex3Hash = vertex3Hash;
    }
}
