package celtech.services.modelLoader;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
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
    private final DoubleProperty percentProgress = new SimpleDoubleProperty();

    public ModelLoaderTask(List<File> modelFilesToLoad)
    {
        this.modelFilesToLoad = modelFilesToLoad;

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

        updateTitle(Lookup.i18n("dialogs.loadModelTitle"));

        for (File modelFileToLoad : modelFilesToLoad)
        {
            steno.info("Model file load started:" + modelFileToLoad.getName());

            ModelLoadResult modelLoadResult = null;
            String modelFilePath = modelFileToLoad.getAbsolutePath();
            updateMessage(Lookup.i18n("dialogs.gcodeLoadMessagePrefix")
                + modelFileToLoad.getName());
            updateProgress(0, 100);
            if (modelFilePath.toUpperCase().endsWith("OBJ"))
            {
                ObjImporter reader = new ObjImporter();
                modelLoadResult = reader.loadFile(this, modelFilePath, percentProgress, false);
            } else if (modelFilePath.toUpperCase().endsWith("STL"))
            {
                STLImporter reader = new STLImporter();
                modelLoadResult = reader.loadFile(this, modelFileToLoad,
                                                  percentProgress);
            }
            modelLoadResultList.add(modelLoadResult);
        }

        return new ModelLoadResults(modelLoadResultList);
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
