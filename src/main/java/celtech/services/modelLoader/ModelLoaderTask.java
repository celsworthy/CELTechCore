/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.modelLoader;

import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ProjectTab;
import celtech.coreUI.visualisation.importers.obj.ObjImporter;
import celtech.coreUI.visualisation.importers.stl.STLImporter;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.coreUI.visualisation.importers.gcode.GCodeImporter;
import celtech.coreUI.visualisation.importers.gcode.GCodeImporterLines;
import java.util.ResourceBundle;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class ModelLoaderTask extends Task<ModelLoadResult>
{

    private Stenographer steno = StenographerFactory.getStenographer(ModelLoaderTask.class.getName());

    private String modelFileToLoad = null;
    private String shortModelName = null;
    private ProjectTab targetProjectTab = null;
    private ResourceBundle languageBundle = null;
    private DoubleProperty percentProgress = new SimpleDoubleProperty();

    public ModelLoaderTask(String modelFileToLoad, String shortModelName, ProjectTab targetProjectTab)
    {
        this.modelFileToLoad = modelFileToLoad;
        this.shortModelName = shortModelName;
        this.targetProjectTab = targetProjectTab;
        languageBundle = DisplayManager.getLanguageBundle();

        percentProgress.addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                updateProgress(t1.doubleValue(), 100.0);
            }
        });
    }

    @Override
    protected ModelLoadResult call() throws Exception
    {
        ModelLoadResult modelLoadResult = null;
        
        steno.debug("About to load " + modelFileToLoad);
        updateTitle(languageBundle.getString("dialogs.loadModelTitle"));
        updateMessage(languageBundle.getString("dialogs.gcodeLoadMessagePrefix") + shortModelName);
        updateProgress(0, 100);

        if (modelFileToLoad.endsWith("obj") || modelFileToLoad.endsWith("OBJ"))
        {
            ObjImporter reader = new ObjImporter();
            modelLoadResult = reader.loadFile(this, "file://" + modelFileToLoad, targetProjectTab);
        } else if (modelFileToLoad.endsWith("stl") || modelFileToLoad.endsWith("STL"))
        {
            STLImporter reader = new STLImporter();
            modelLoadResult = reader.loadFile(this, modelFileToLoad, targetProjectTab, percentProgress);
        }
        else if (modelFileToLoad.endsWith("gcode") || modelFileToLoad.endsWith("GCODE"))
        {
            GCodeImporterLines reader = new GCodeImporterLines();
            modelLoadResult = reader.loadFile(this, modelFileToLoad, targetProjectTab, percentProgress);
        }

        return modelLoadResult;
    }
    
    public void updateMessageText(String message)
    {
        updateMessage(message);
    }
}
