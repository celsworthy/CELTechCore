package celtech.coreUI.controllers;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.Spinner;
import celtech.coreUI.components.buttons.GraphicButton;
import celtech.utils.MyMiniFactoryLoadResult;
import celtech.utils.MyMiniFactoryLoader;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import netscape.javascript.JSObject;

/**
 *
 * @author Ian
 */
public class MyMiniFactoryLoaderController implements Initializable
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
        MyMiniFactoryLoaderController.class.getName());

    private WebEngine webEngine = null;

    private Spinner spinner = null;

    private final StringProperty fileDownloadLocation = new SimpleStringProperty("");

    @FXML
    private VBox webContentContainer;

    @FXML
    private GraphicButton addToProjectButton;

    @FXML
    private GraphicButton forwardButton;

    @FXML
    private GraphicButton backwardButton;

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
    void forwardPressed(ActionEvent event)
    {
        JSObject history = (JSObject) webEngine.executeScript("history");
        history.call("forward");
    }

    @FXML
    void addToProjectPressed(ActionEvent event)
    {
        Platform.runLater(() ->
        {
            downloadFile(fileDownloadLocation.get());
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        spinner = new Spinner();

        addToProjectButton.disableProperty().bind(Bindings.equal("", fileDownloadLocation));

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
                        fileDownloadLocation.set("");
                        spinner.startSpinning();
                        break;
                    case SUCCEEDED:
                        fileDownloadLocation.set("");
                        spinner.stopSpinning();
                        Object fileLinkFunction = webEngine
                        .executeScript("window.autoMakerGetFileLink");
                        if (fileLinkFunction instanceof JSObject)
                        {
                            fileDownloadLocation.set((String) webEngine
                                .executeScript("window.autoMakerGetFileLink()"));
                        }
                        boolean okForBackwards = webEngine.getLocation().matches(".*\\/object\\/.*");
                        boolean okForForwards = !okForBackwards && webEngine.getHistory().getEntries().size() > 0;
                        backwardButton.disableProperty().set(!okForBackwards);
                        forwardButton.disableProperty().set(!okForForwards);
                        break;
                    case CANCELLED:
                        fileDownloadLocation.set("");
                        spinner.stopSpinning();
                        break;
                    case FAILED:
                        fileDownloadLocation.set("");
                        spinner.stopSpinning();
                        break;
                }
            });
        webContentContainer.getChildren().addAll(webView);
        webEngine.load("http://cel-robox.myminifactory.com");
    }

    private boolean alreadyDownloading = false;

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
                    DisplayManager.getInstance().loadExternalModels(result.getFilesToLoad());
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

    private void finishedWithEngines()
    {
        alreadyDownloading = false;
        spinner.stopSpinning();
    }

}
