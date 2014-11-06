package celtech.coreUI.controllers;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.ProjectMode;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.DirectoryMemoryProperty;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.Spinner;
import celtech.coreUI.components.buttons.GraphicButton;
import celtech.utils.MyMiniFactoryLoadResult;
import celtech.utils.MyMiniFactoryLoader;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import netscape.javascript.JSObject;

/**
 *
 * @author Ian
 */
public class MyMiniFactoryLoaderController implements Initializable
{

    private final FileChooser modelFileChooser = new FileChooser();
    private DisplayManager displayManager = null;
    private ResourceBundle i18nBundle = null;
    private static final Stenographer steno = StenographerFactory.getStenographer(MyMiniFactoryLoaderController.class.getName());

    private WebEngine webEngine = null;

    private Spinner spinner = null;

    @FXML
    private VBox webContentContainer;

    @FXML
    private GraphicButton addToProjectButton;

    @FXML
    void cancelPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().modeProperty().set(ApplicationMode.LAYOUT);
    }

    @FXML
    void backwardPressed(ActionEvent event)
    {
        JSObject history = (JSObject) webEngine.executeScript("history");
        history.call("back");
    }

    @FXML
    void addToProjectPressed(ActionEvent event)
    {
        Platform.runLater(() ->
        {
            ListIterator iterator = modelFileChooser.getExtensionFilters().listIterator();

            while (iterator.hasNext())
            {
                iterator.next();
                iterator.remove();
            }

            ProjectMode projectMode = ProjectMode.NONE;

            if (displayManager.getCurrentlyVisibleProject() != null)
            {
                projectMode = displayManager.getCurrentlyVisibleProject().getProjectMode();
            }

            String descriptionOfFile = null;

            switch (projectMode)
            {
                case NONE:
                    descriptionOfFile = i18nBundle.getString("dialogs.anyFileChooserDescription");
                    break;
                case MESH:
                    descriptionOfFile = i18nBundle.getString("dialogs.meshFileChooserDescription");
                    break;
                case GCODE:
                    descriptionOfFile = i18nBundle.getString("dialogs.gcodeFileChooserDescription");
                    break;
                default:
                    break;
            }
            modelFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(descriptionOfFile,
                                                ApplicationConfiguration.getSupportedFileExtensionWildcards(
                                                    projectMode)));

            modelFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(
                DirectoryMemoryProperty.MODEL)));

            List<File> files;
            if (projectMode == ProjectMode.NONE || projectMode == ProjectMode.MESH)
            {
                files = modelFileChooser.showOpenMultipleDialog(displayManager.getMainStage());
            } else
            {
                File file = modelFileChooser.showOpenDialog(displayManager.getMainStage());
                files = new ArrayList<>();
                if (file != null)
                {
                    files.add(file);
                }
            }

            if (files != null && !files.isEmpty())
            {
                ApplicationConfiguration.setLastDirectory(
                    DirectoryMemoryProperty.MODEL,
                    files.get(0).getParentFile().getAbsolutePath());
                displayManager.loadExternalModels(files, true);
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        displayManager = DisplayManager.getInstance();
        i18nBundle = DisplayManager.getLanguageBundle();

        modelFileChooser.setTitle(i18nBundle.getString("dialogs.modelFileChooser"));
        modelFileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(i18nBundle.getString("dialogs.modelFileChooserDescription"), ApplicationConfiguration.getSupportedFileExtensionWildcards(ProjectMode.NONE)));

        spinner = new Spinner();

        addToProjectButton.setDisable(true);
        
        loadWebData();

    }

    public void loadWebData()
    {
        webContentContainer.getChildren().clear();

        WebView webView = new WebView();
        VBox.setVgrow(webView, Priority.ALWAYS);

        spinner.startSpinning();

        webEngine = webView.getEngine();

        webEngine.getLoadWorker().stateProperty().addListener(
            (ObservableValue<? extends Worker.State> ov, Worker.State oldState, Worker.State newState) ->
            {
                switch (newState)
                {
                    case RUNNING:
                        spinner.startSpinning();
                        steno.info("running");
                        break;
                    case SUCCEEDED:
                        spinner.stopSpinning();
                        steno.info("loaded");
                        JSObject win = (JSObject) webEngine.executeScript("window");
                        win.setMember("automaker", new WebCallback());
                        break;
                    case CANCELLED:
                        spinner.stopSpinning();
                        steno.info("cancelled");
                        break;
                    case FAILED:
                        spinner.stopSpinning();
                        steno.info("failed");
                        break;
                }
            });
        webContentContainer.getChildren().addAll(webView);
        webEngine.load("http://cel-robox.myminifactory.com");
    }

    private boolean alreadyDownloading = false;

    public class WebCallback
    {

        public void downloadFile(String fileURL)
        {
            if (!alreadyDownloading)
            {
                alreadyDownloading = true;
                spinner.startSpinning();

                MyMiniFactoryLoader loader = new MyMiniFactoryLoader(fileURL);

                loader.setOnSucceeded((WorkerStateEvent event) ->
                {
                    MyMiniFactoryLoadResult result = (MyMiniFactoryLoadResult) event.getSource().getValue();
                    if (result.isSuccess())
                    {
                        displayManager.loadExternalModels(result.getFilesToLoad());
                    }
                    finishedWithEngines();
                    ApplicationStatus.getInstance().setMode(ApplicationMode.LAYOUT);
                });

                loader.setOnFailed((WorkerStateEvent event) ->
                {
                    finishedWithEngines();
                });

                loader.setOnCancelled((WorkerStateEvent event) ->
                {
                    finishedWithEngines();
                });

                Thread loaderThread = new Thread(loader);
                loaderThread.start();
            }
        }
    }

    private void finishedWithEngines()
    {
        alreadyDownloading = false;
        spinner.stopSpinning();
    }

}
