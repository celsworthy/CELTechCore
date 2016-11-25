package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.WebEngineFix.AMURLStreamHandlerFactory;
import celtech.coreUI.components.ChoiceLinkDialogBox;
import celtech.coreUI.components.RootTableCell;
import celtech.roboxbase.comms.DetectedDevice;
import celtech.roboxbase.comms.DetectedServer;
import celtech.roboxbase.comms.DetectedServer.ServerStatus;
import celtech.roboxbase.comms.DeviceDetectionListener;
import celtech.roboxbase.comms.RemoteServerDetector;
import celtech.roboxbase.comms.remote.Configuration;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.CoreMemory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
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
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
    private Button disconnectButton;

    @FXML
    private TextField pinEntryField;

    @FXML
    private Label incorrectPINLabel;

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
    private TableColumn<DetectedServer, ServerStatus> statusColumn;

    private WebView rootWebView;

    private ObservableList<DetectedServer> currentServers = FXCollections.observableArrayList();

    private ChangeListener<ServerStatus> serverStatusListener = new ChangeListener<ServerStatus>()
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
                connectPage.setVisible(false);
                connectPage.setMouseTransparent(true);
                disconnectButton.setVisible(false);
                disconnectButton.setMouseTransparent(true);
                incorrectPINLabel.setVisible(false);
                wrongVersionBox.setVisible(true);
                wrongVersionBox.setMouseTransparent(false);
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
        nameColumn.setPrefWidth(180);
        nameColumn.setResizable(false);
        nameColumn.setStyle("-fx-alignment: CENTER_LEFT;");

        statusColumn = new TableColumn<>();
        statusColumn.setCellFactory(statusCell -> new RootTableCell());
        statusColumn.setCellValueFactory(new PropertyValueFactory<DetectedServer, ServerStatus>("serverStatus"));
        statusColumn.setPrefWidth(40);
        statusColumn.setResizable(false);

        scannedRoots.getColumns().add(nameColumn);
        scannedRoots.getColumns().add(statusColumn);
        scannedRoots.setMaxWidth(220);

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

        if (currentServers.size() > 0)
        {
            scannedRoots.getSelectionModel().selectFirst();
        }

        List<DetectedServer> rootsToInhibit = new ArrayList<>();

        List<DetectedServer> serversToCheck = new ArrayList<>(CoreMemory.getInstance().getActiveRoboxRoots());
        serversToCheck.forEach((server) ->
        {
            if (!server.whoAmI())
            {
                rootsToInhibit.add(server);
            } else
            {
                server.connect();
                currentServers.add(server);
            }
        });

        if (currentServers.size() > 0)
        {
            scannedRoots.getSelectionModel().selectFirst();
        }

        rootsToInhibit.forEach(server ->
        {
            CoreMemory.getInstance().deactivateRoboxRoot(server);
        });

        Task<Void> scannerTask = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                while (!isCancelled())
                {
                    List<DetectedServer> foundServers = remoteServerDetector.searchForServers();
                    Platform.runLater(() ->
                    {
                        List<DetectedServer> serversToAdd = new ArrayList<>();
                        List<DetectedServer> serversToRemove = new ArrayList<>();

                        for (DetectedServer server : foundServers)
                        {
                            if (!currentServers.contains(server))
                            {
                                serversToAdd.add(server);
                            }
                        }

                        for (DetectedServer server : currentServers)
                        {
                            if (!foundServers.contains(server))
                            {
                                serversToRemove.add(server);
                            }
                        }

                        for (DetectedServer server : serversToAdd)
                        {
                            currentServers.add(server);
                        }
                        for (DetectedServer server : serversToRemove)
                        {
                            currentServers.remove(server);
                        }
                    });
                }

                return null;
            }
        };

        Thread scannerThread = new Thread(scannerTask);
        scannerThread.setDaemon(true);
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
