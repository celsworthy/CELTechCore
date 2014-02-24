/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import java.io.IOException;

/**
 *
 * @author ianhudson
 */
public class SlicedFile
{

    private String UUIDString = null;
    private final String UUIDStringTag = "fileUUID";

    public SlicedFile()
    {
    }
    
    public SlicedFile(String UUIDString)
    {
        this.UUIDString = UUIDString;
    }
    
    public String getUUIDString()
    {
        return UUIDString;
    }

    public void setUUIDString(String UUIDString)
    {
        this.UUIDString = UUIDString;
    }
}
