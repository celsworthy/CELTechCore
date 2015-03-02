/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.modelLoader;

import celtech.Lookup;
import celtech.coreUI.components.ProjectTab;
import celtech.coreUI.visualisation.importers.obj.ObjImporter;
import celtech.coreUI.visualisation.importers.stl.STLImporter;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.coreUI.visualisation.importers.gcode.GCodeImporterLines;
import celtech.coreUI.visualisation.importers.stl.STLImporter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
public class ModelLoaderTask extends Task<ModelLoadResults>
{

    private Stenographer steno = StenographerFactory.getStenographer(ModelLoaderTask.class.getName());

    private final List<File> modelFilesToLoad;
    private ProjectTab targetProjectTab = null;
    private ResourceBundle languageBundle = null;
    private final DoubleProperty percentProgress = new SimpleDoubleProperty();
    private final boolean relayout;

    public ModelLoaderTask(List<File> modelFilesToLoad, ProjectTab targetProjectTab,
        boolean relayout)
    {
        this.modelFilesToLoad = modelFilesToLoad;
        this.relayout = relayout;
        this.targetProjectTab = targetProjectTab;
        languageBundle = Lookup.getLanguageBundle();

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
    protected ModelLoadResults call() throws Exception
    {
        List<ModelLoadResult> modelLoadResultList = new ArrayList<>();

        updateTitle(languageBundle.getString("dialogs.loadModelTitle"));

        for (File modelFileToLoad : modelFilesToLoad)
        {
            ModelLoadResult modelLoadResult = null;
            String modelFilePath = modelFileToLoad.getAbsolutePath();
            updateMessage(languageBundle.getString("dialogs.gcodeLoadMessagePrefix")
                + modelFileToLoad.getName());
            updateProgress(0, 100);
            if (modelFilePath.toUpperCase().endsWith("OBJ"))
            {
                ObjImporter reader = new ObjImporter();
                modelLoadResult = reader.loadFile(this, "file:///" + modelFilePath,
                                                  targetProjectTab);
            } else if (modelFilePath.toUpperCase().endsWith("STL"))
            {
                STLImporter reader = new STLImporter();
                modelLoadResult = reader.loadFile(this, modelFilePath, targetProjectTab,
                                                  percentProgress);
            } else if (modelFilePath.toUpperCase().endsWith("GCODE"))
            {
                GCodeImporterLines reader = new GCodeImporterLines();
                modelLoadResult = reader.loadFile(this, modelFilePath, targetProjectTab,
                                                  percentProgress);
            }
            modelLoadResultList.add(modelLoadResult);
        }

        return new ModelLoadResults(modelLoadResultList, relayout);
    }

    /**
     *
     * @param message
     */
    public void updateMessageText(String message)
    {
        updateMessage(message);
    }
}
