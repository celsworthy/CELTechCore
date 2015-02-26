/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.utils.SystemUtils;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProjectHeader implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final transient SimpleDateFormat formatter = new SimpleDateFormat("-hhmmss-ddMMYY");
    private String projectUUID = null;
    private StringProperty projectNameProperty = null;
    private String projectPath = null;
    private final ObjectProperty<Date> lastModifiedDate = new SimpleObjectProperty<>();

    public ProjectHeader()
    {
        projectUUID = SystemUtils.generate16DigitID();
        Date now = new Date();
        projectNameProperty = new SimpleStringProperty(Lookup.i18n("projectLoader.untitled") + formatter.format(now));
        projectPath = ApplicationConfiguration.getProjectDirectory();
        lastModifiedDate.set(now);
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

    public final String getProjectPath()
    {
        return projectPath;
    }

    public void setProjectPath(String value)
    {
        projectPath = value;
    }

    public final String getUUID()
    {
        return projectUUID;
    }

    public final void setProjectUUID(String value)
    {
        projectUUID = value;
    }

    public final Date getLastModifiedDate()
    {
        return lastModifiedDate.get();
    }

    public final ObjectProperty<Date> getLastModifiedDateProperty()
    {
        return lastModifiedDate;
    }

//    private void writeObject(ObjectOutputStream out)
//            throws IOException
//    {
//        out.writeUTF(projectUUID);
//        out.writeUTF(projectNameProperty.get());
//        out.writeUTF(projectPath);
//        out.writeObject(lastModifiedDate.get());
//        out.writeObject(new Date());
//    }
//
//    private void readObject(ObjectInputStream in)
//            throws IOException, ClassNotFoundException
//    {
//        projectUUID = in.readUTF();
//        projectNameProperty = new SimpleStringProperty(in.readUTF());
//        projectPath = in.readUTF();
//        lastModifiedDate = new SimpleObjectProperty<>((Date)(in.readObject()));
//        lastSavedDate = new SimpleObjectProperty<>((Date)(in.readObject()));
//    }
//
//    private void readObjectNoData()
//            throws ObjectStreamException
//    {
//
//    }

}
