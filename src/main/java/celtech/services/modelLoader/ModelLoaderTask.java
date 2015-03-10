package celtech.services.modelLoader;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import celtech.utils.threed.importers.gcode.GCodeImporterLines;
import celtech.utils.threed.importers.obj.ObjImporter;
import celtech.utils.threed.importers.stl.STLImporter;
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

    private Stenographer steno = StenographerFactory.
        getStenographer(ModelLoaderTask.class.getName());

    private final List<File> modelFilesToLoad;
    private Project targetProject;
    private ResourceBundle languageBundle = null;
    private final DoubleProperty percentProgress = new SimpleDoubleProperty();
    private final boolean relayout;

    public ModelLoaderTask(List<File> modelFilesToLoad, Project targetProject,
        boolean relayout)
    {
        this.modelFilesToLoad = modelFilesToLoad;
        this.relayout = relayout;
        this.targetProject = targetProject;
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
            steno.info("Model file load started:" + modelFileToLoad.getName());

            ModelLoadResult modelLoadResult = null;
            String modelFilePath = modelFileToLoad.getAbsolutePath();
            updateMessage(languageBundle.getString("dialogs.gcodeLoadMessagePrefix")
                + modelFileToLoad.getName());
            updateProgress(0, 100);
            if (modelFilePath.toUpperCase().endsWith("OBJ"))
            {
                ObjImporter reader = new ObjImporter();
                modelLoadResult = reader.loadFile(this, "file:///" + modelFilePath,
                                                  targetProject);
            } else if (modelFilePath.toUpperCase().endsWith("STL"))
            {
                STLImporter reader = new STLImporter();
                modelLoadResult = reader.loadFile(this, modelFileToLoad, targetProject,
                                                  percentProgress);
            } else if (modelFilePath.toUpperCase().endsWith("GCODE"))
            {
                GCodeImporterLines reader = new GCodeImporterLines();
                modelLoadResult = reader.loadFile(this, modelFilePath, targetProject,
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
