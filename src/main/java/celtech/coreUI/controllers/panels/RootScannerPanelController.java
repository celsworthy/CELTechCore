package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.WebEngineFix.AMURLStreamHandlerFactory;
import celtech.coreUI.components.Notifications.GenericProgressBar;
import celtech.coreUI.components.RootTableCell;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.comms.DetectedDevice;
import celtech.roboxbase.comms.DetectedServer;
import celtech.roboxbase.comms.DetectedServer.ServerStatus;
import celtech.roboxbase.comms.DeviceDetectionListener;
import celtech.roboxbase.comms.RemoteServerDetector;
import celtech.roboxbase.comms.remote.Configuration;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.CoreMemory;
import celtech.roboxbase.utils.SystemUtils;
import celtech.utils.TaskWithProgessCallback;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class RootScannerPanelController implements Initializable, MenuInnerPanel, DeviceDetectionListener
{

    private static final Stenographer steno = StenographerFactory.getStenographer(RootScannerPanelController.class.getName());

    private final RemoteServerDetector remoteServerDetector = RemoteServerDetector.getInstance();

    public static String pinForCurrentServer = "";

    private GenericProgressBar rootSoftwareUpDownloadProgress;

    @FXML
    private HBox scannerHolder;

    @FXML
    private TableView<DetectedServer> scannedRoots;

    @FXML
    private VBox webViewHolder;

    @FXML
    private VBox connectPage;

    @FXML
    private VBox wrongVersionBox;

    @FXML
    private Label rootVersionLabel;

    @FXML
    private Button rootUpdateButton;

    @FXML
    private Button disconnectButton;

    @FXML
    private TextField pinEntryField;

    @FXML
    private Label incorrectPINLabel;

    @FXML
    void beginRootUpdate(ActionEvent event)
    {
        //Is root file there?
        //The format is RootARM-32bit-2.02.00_RC8.zip
        String pathToRootFile = BaseConfiguration.getUserTempDirectory();
        String rootFile = "RootARM-32bit-" + BaseConfiguration.getApplicationVersion() + ".zip";
        Path rootFilePath = Paths.get(pathToRootFile + rootFile);

        if (Files.exists(rootFilePath, LinkOption.NOFOLLOW_LINKS))
        {
            //Use this file to upgrade the root
            upgradeRootWithFile(pathToRootFile, rootFile);
        } else
        {
            //We need to download it
            TaskWithProgessCallback<Boolean> rootDownloader = new TaskWithProgessCallback<Boolean>()
            {
                @Override
                protected Boolean call() throws Exception
                {
                    URL obj = new URL("http://www.cel-robox.com/wp-content/uploads/Software/Root/" + rootFile);
                    boolean success = SystemUtils.downloadFromUrl(obj, pathToRootFile + rootFile, this);
                    return success;
                }

                @Override
                public void updateProgressPercent(double percentProgress)
                {
                    updateProgress(percentProgress, 100.0);
                }
            };

            rootDownloader.setOnSucceeded((result) ->
            {
                BaseLookup.getSystemNotificationHandler().showErrorNotification(Lookup.i18n("rootScanner.rootDownloadTitle"), Lookup.i18n("rootScanner.successfulDownloadMessage"));
                upgradeRootWithFile(pathToRootFile, rootFile);
                Lookup.getProgressDisplay().removeGenericProgressBarFromDisplay(rootSoftwareUpDownloadProgress);
                rootSoftwareUpDownloadProgress = null;
            });

            rootDownloader.setOnFailed((result) ->
            {
                BaseLookup.getSystemNotificationHandler().showErrorNotification(Lookup.i18n("rootScanner.rootDownloadTitle"), Lookup.i18n("rootScanner.failedDownloadMessage"));
                Lookup.getProgressDisplay().removeGenericProgressBarFromDisplay(rootSoftwareUpDownloadProgress);
                rootSoftwareUpDownloadProgress = null;
            });

            rootUpdateButton.disableProperty().unbind();
            rootUpdateButton.disableProperty().bind(rootDownloader.runningProperty());

            if (rootSoftwareUpDownloadProgress != null)
            {
                Lookup.getProgressDisplay().removeGenericProgressBarFromDisplay(rootSoftwareUpDownloadProgress);
                rootSoftwareUpDownloadProgress = null;
            }

            Lookup.getProgressDisplay().addGenericProgressBarToDisplay(Lookup.i18n("rootScanner.rootDownloadTitle"),
                    rootDownloader.runningProperty(),
                    rootDownloader.progressProperty());

            Thread rootDownloaderThread = new Thread(rootDownloader);
            rootDownloaderThread.setName("Root software downloader");
            rootDownloaderThread.setDaemon(true);
            rootDownloaderThread.start();
        }
    }

    @FXML
    void connectToServer(ActionEvent event)
    {
        DetectedServer server = scannedRoots.getSelectionModel().getSelectedItem();
        if (server != null
                && server.getServerStatus() != ServerStatus.CONNECTED)
        {
            server.setPin(pinEntryField.getText());
            server.connect();
        }
    }

    @FXML
    void disconnectFromServer(ActionEvent event)
    {
        DetectedServer server = scannedRoots.getSelectionModel().getSelectedItem();
        if (server != null
                && server.getServerStatus() == ServerStatus.CONNECTED)
        {
            server.disconnect();
        }
    }

    private TableColumn nameColumn;
    private TableColumn ipAddressColumn;
    private TableColumn versionColumn;
    private TableColumn<DetectedServer, ServerStatus> statusColumn;

    private WebView rootWebView;

    private ObservableList<DetectedServer> currentServers = FXCollections.observableArrayList();

    private final ChangeListener<ServerStatus> serverStatusListener = new ChangeListener<ServerStatus>()
    {
        @Override
        public void changed(ObservableValue<? extends ServerStatus> observable, ServerStatus oldValue, ServerStatus newValue)
        {
            processStatus(newValue);
        }
    };

    private void openWebViewOnRoot(DetectedServer server)
    {
        String url = "http://" + server.getAddress().getHostAddress() + ":" + Configuration.remotePort + "/login.html";

        pinForCurrentServer = server.getPin();

        rootWebView.getEngine().load(url);
    }

    public class JavaBridge
    {

        public void log(String text)
        {
            steno.info(text);
        }
    }

    private void upgradeRootWithFile(String path, String filename)
    {
        DetectedServer server = scannedRoots.getSelectionModel().getSelectedItem();
        if (server != null
                && server.getServerStatus() == ServerStatus.WRONG_VERSION)
        {
            TaskWithProgessCallback<Boolean> rootUploader = new TaskWithProgessCallback<Boolean>()
            {
                @Override
                protected Boolean call() throws Exception
                {
                    return server.upgradeRootSoftware(path, filename, this);
                }

                @Override
                public void updateProgressPercent(double percentProgress)
                {
                    updateProgress(percentProgress, 100.0);
                }
            };

            rootUploader.setOnFailed((event) ->
            {
                BaseLookup.getSystemNotificationHandler().showErrorNotification(Lookup.i18n("rootScanner.rootUploadTitle"), Lookup.i18n("rootScanner.failedUploadMessage"));
                Lookup.getProgressDisplay().removeGenericProgressBarFromDisplay(rootSoftwareUpDownloadProgress);
                rootSoftwareUpDownloadProgress = null;
            });

            rootUploader.setOnSucceeded((event) ->
            {
                if ((boolean) event.getSource().getValue())
                {
                    BaseLookup.getSystemNotificationHandler().showErrorNotification(Lookup.i18n("rootScanner.rootUploadTitle"), Lookup.i18n("rootScanner.successfulUploadMessage"));
                } else
                {
                    BaseLookup.getSystemNotificationHandler().showErrorNotification(Lookup.i18n("rootScanner.rootUploadTitle"), Lookup.i18n("rootScanner.failedUploadMessage"));
                }
                Lookup.getProgressDisplay().removeGenericProgressBarFromDisplay(rootSoftwareUpDownloadProgress);
                rootSoftwareUpDownloadProgress = null;
            });

            rootUpdateButton.disableProperty().unbind();
            rootUpdateButton.disableProperty().bind(rootUploader.runningProperty());

            if (rootSoftwareUpDownloadProgress != null)
            {
                Lookup.getProgressDisplay().removeGenericProgressBarFromDisplay(rootSoftwareUpDownloadProgress);
                rootSoftwareUpDownloadProgress = null;
            }

            Lookup.getProgressDisplay().addGenericProgressBarToDisplay(Lookup.i18n("rootScanner.rootUploadTitle"),
                    rootUploader.runningProperty(),
                    rootUploader.progressProperty());

            Thread rootUploaderThread = new Thread(rootUploader);
            rootUploaderThread.setName("Root uploader");
            rootUploaderThread.setDaemon(true);
            rootUploaderThread.start();
        }
    }

    private void hideEverything()
    {
        webViewHolder.setVisible(false);
        webViewHolder.setMouseTransparent(true);
        connectPage.setVisible(false);
        connectPage.setMouseTransparent(true);
        disconnectButton.setVisible(false);
        disconnectButton.setMouseTransparent(true);
        incorrectPINLabel.setVisible(false);
        wrongVersionBox.setVisible(false);
        wrongVersionBox.setMouseTransparent(true);
    }

    private void processStatus(ServerStatus status)
    {
        switch (status)
        {
            case CONNECTED:
                webViewHolder.setVisible(true);
                webViewHolder.setMouseTransparent(false);
                connectPage.setVisible(false);
                connectPage.setMouseTransparent(true);
                disconnectButton.setVisible(true);
                disconnectButton.setMouseTransparent(false);
                incorrectPINLabel.setVisible(false);
                wrongVersionBox.setVisible(false);
                wrongVersionBox.setMouseTransparent(true);
                openWebViewOnRoot(scannedRoots.getSelectionModel().getSelectedItem());
                break;
            case WRONG_PIN:
                webViewHolder.setVisible(false);
                webViewHolder.setMouseTransparent(true);
                connectPage.setVisible(true);
                connectPage.setMouseTransparent(false);
                disconnectButton.setVisible(false);
                disconnectButton.setMouseTransparent(true);
                incorrectPINLabel.setVisible(true);
                wrongVersionBox.setVisible(false);
                wrongVersionBox.setMouseTransparent(true);
                break;
            case WRONG_VERSION:
                webViewHolder.setVisible(false);
                webViewHolder.setMouseTransparent(true);
                connectPage.setVisible(true);
                connectPage.setMouseTransparent(false);
                disconnectButton.setVisible(false);
                disconnectButton.setMouseTransparent(true);
                incorrectPINLabel.setVisible(false);
                wrongVersionBox.setVisible(true);
                wrongVersionBox.setMouseTransparent(false);
                rootVersionLabel.setText(Lookup.i18n("rootScanner.wrongVersion"));
                break;
            case NOT_CONNECTED:
                webViewHolder.setVisible(false);
                webViewHolder.setMouseTransparent(true);
                connectPage.setVisible(true);
                connectPage.setMouseTransparent(false);
                disconnectButton.setVisible(false);
                disconnectButton.setMouseTransparent(true);
                incorrectPINLabel.setVisible(false);
                wrongVersionBox.setVisible(false);
                wrongVersionBox.setMouseTransparent(true);
                break;
            default:
                hideEverything();
                break;
        }
    }

    /**
     * Initialises the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        URL.setURLStreamHandlerFactory(new AMURLStreamHandlerFactory());

        nameColumn = new TableColumn<>();
        nameColumn.setCellValueFactory(new PropertyValueFactory<DetectedServer, String>("name"));
        nameColumn.setText(Lookup.i18n("rootScanner.name"));
        nameColumn.setPrefWidth(160);
        nameColumn.setResizable(false);
        nameColumn.setStyle("-fx-alignment: CENTER_LEFT;");

        ipAddressColumn = new TableColumn<>();
        ipAddressColumn.setCellValueFactory(new PropertyValueFactory<DetectedServer, String>("serverIP"));
        ipAddressColumn.setText(Lookup.i18n("rootScanner.ipAddress"));
        ipAddressColumn.setPrefWidth(100);
        ipAddressColumn.setResizable(false);
        ipAddressColumn.setStyle("-fx-alignment: CENTER;");

        versionColumn = new TableColumn<>();
        versionColumn.setCellValueFactory(new PropertyValueFactory<DetectedServer, String>("version"));
        versionColumn.setText(Lookup.i18n("rootScanner.version"));
        versionColumn.setPrefWidth(85);
        versionColumn.setResizable(false);
        versionColumn.setStyle("-fx-alignment: CENTER;");

        statusColumn = new TableColumn<>();
        statusColumn.setCellFactory(statusCell -> new RootTableCell());
        statusColumn.setCellValueFactory(new PropertyValueFactory<DetectedServer, ServerStatus>("serverStatus"));
        statusColumn.setPrefWidth(40);
        statusColumn.setResizable(false);

        scannedRoots.getColumns().add(nameColumn);
        scannedRoots.getColumns().add(ipAddressColumn);
        scannedRoots.getColumns().add(versionColumn);
        scannedRoots.getColumns().add(statusColumn);
        scannedRoots.setMaxWidth(385);

        rootWebView = new WebView();
        rootWebView.setMaxHeight(1000000);
        webViewHolder.getChildren().add(rootWebView);

        VBox.setVgrow(webViewHolder, Priority.ALWAYS);

        AnchorPane.setBottomAnchor(rootWebView, 0.0);
        AnchorPane.setLeftAnchor(rootWebView, 0.0);
        AnchorPane.setRightAnchor(rootWebView, 0.0);
        AnchorPane.setTopAnchor(rootWebView, 0.0);
        rootWebView.getEngine().setJavaScriptEnabled(true);
        rootWebView.getEngine().setUserDataDirectory(new File(BaseConfiguration.getUserStorageDirectory()));

        rootWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) ->
        {
            JSObject window = (JSObject) rootWebView.getEngine().executeScript("window");
            JavaBridge bridge = new JavaBridge();
            window.setMember("java", bridge);
            rootWebView.getEngine().executeScript("console.log = function(message)\n"
                    + "{\n"
                    + "    java.log(message);\n"
                    + "};");
        });

        rootWebView.getEngine().documentProperty().addListener(new ChangeListener<Document>()
        {
            @Override
            public void changed(ObservableValue<? extends Document> ov, Document oldDoc, Document doc)
            {
                if (doc != null)
                {
                    if (doc.getDocumentURI().endsWith("login.html"))
                    {
                        HTMLFormElement pinInputForm = (HTMLFormElement) doc.getElementById("pinInputForm");
                        HTMLInputElement pinInput = (HTMLInputElement) doc.getElementById("application-pin-value");

                        pinInput.setValue(pinForCurrentServer);
                        pinInputForm.submit();
                    }

                }
            }
        });

        scannedRoots.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        scannedRoots.setItems(currentServers);

        scannedRoots.setPlaceholder(new Text(BaseLookup.i18n("rootScanner.noRemoteServersFound")));

        hideEverything();

        currentServers.addListener(new ListChangeListener<DetectedServer>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends DetectedServer> change)
            {
                if (currentServers.size() > 0)
                {
                    scannedRoots.getSelectionModel().selectFirst();
                } else
                {
                    scannedRoots.getSelectionModel().clearSelection();
                }
            }
        });

        scannedRoots.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<DetectedServer>()
        {
            @Override
            public void changed(ObservableValue<? extends DetectedServer> observable, DetectedServer oldValue, DetectedServer newValue)
            {
                if (oldValue != null)
                {
                    oldValue.serverStatusProperty().removeListener(serverStatusListener);
                }
                if (newValue != null)
                {
                    newValue.serverStatusProperty().addListener(serverStatusListener);
                    processStatus(newValue.getServerStatus());
                } else
                {
                    hideEverything();
                }
            }
        });

        Task<Void> scannerTask = new Task<Void>()
        {
            private List<DetectedServer> currentServerList = new ArrayList<>();

            @Override
            protected Void call() throws Exception
            {
                List<DetectedServer> serversToCheck = new ArrayList<>(CoreMemory.getInstance().getActiveRoboxRoots());
                serversToCheck.forEach((server) ->
                {
                    if (!server.whoAreYou())
                    {
                        CoreMemory.getInstance().deactivateRoboxRoot(server);
                    } else
                    {
                        server.connect();
                        Platform.runLater(() ->
                        {
                            currentServerList.add(server);
                            currentServers.add(server);
                        });
                    }
                });

                while (!isCancelled())
                {
                    try
                    {
                        List<DetectedServer> foundServers = remoteServerDetector.searchForServers();

                        Platform.runLater(() ->
                        {
                            List<DetectedServer> serversToAdd = new ArrayList<>();
                            List<DetectedServer> serversToRemove = new ArrayList<>();

                            for (DetectedServer server : foundServers)
                            {
                                if (!currentServerList.contains(server))
                                {
                                    serversToAdd.add(server);
                                }
                            }

                            for (DetectedServer server : currentServerList)
                            {
                                if (!foundServers.contains(server))
                                {
                                    serversToRemove.add(server);
                                }
                            }

                            for (DetectedServer server : serversToAdd)
                            {
                                currentServerList.add(server);
                                currentServers.add(server);
                            }
                            for (DetectedServer server : serversToRemove)
                            {
                                currentServerList.remove(server);
                                currentServers.remove(server);
                            }
                        });
                    } catch (IOException ex)
                    {
                        Thread.sleep(1000);
                    }
                }

                return null;
            }
        };

        Thread scannerThread = new Thread(scannerTask);
        scannerThread.setDaemon(true);
        scannerThread.setName("RootScanner");
        scannerThread.start();
    }

    @Override
    public String getMenuTitle()
    {
        return "preferences.root";
    }

    @Override
    public List<OperationButton> getOperationButtons()
    {
        return null;
    }

    @Override
    public void deviceDetected(DetectedDevice detectedDevice)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deviceNoLongerPresent(DetectedDevice detectedDevice)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
