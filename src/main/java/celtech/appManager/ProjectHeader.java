/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.appManager;

import celtech.coreUI.DisplayManager;
import celtech.utils.SystemUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProjectHeader implements Serializable
{
    private transient SimpleDateFormat formatter = new SimpleDateFormat("-hhmmss-ddMMYY");
    private String projectUUID = null;
    private StringProperty projectNameProperty = new SimpleStringProperty(DisplayManager.getLanguageBundle().getString("projectLoader.untitled") + formatter.format(new Date()));

    public ProjectHeader()
    {
        projectUUID = SystemUtils.generate16DigitID();
    }

    public final void setProjectName(String value)
    {
        projectNameProperty.set(value);
    }

    public final String getProjectName()
    {
        return projectNameProperty.get();
    }

    public final StringProperty projectNameProperty()
    {
        return projectNameProperty;
    }

    public final String getUUID()
    {
        return projectUUID;
    }
    
    public final void setProjectUUID(String value)
    {
        projectUUID = value;
    }
    
    private void writeObject(ObjectOutputStream out)
            throws IOException
    {
        out.writeUTF(projectUUID);
        out.writeUTF(projectNameProperty.get());
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        projectUUID = in.readUTF();
        projectNameProperty=new SimpleStringProperty(in.readUTF());
    }

    private void readObjectNoData()
            throws ObjectStreamException
    {

    }

}
