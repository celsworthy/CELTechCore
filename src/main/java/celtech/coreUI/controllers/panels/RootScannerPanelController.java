package celtech.coreUI.controllers.panels;

import celtech.coreUI.DisplayManager;
import celtech.roboxbase.comms.DetectedDevice;
import celtech.roboxbase.comms.DetectedServer;
import celtech.roboxbase.comms.DetectedServer.ServerStatus;
import celtech.roboxbase.comms.DeviceDetectionListener;
import celtech.roboxbase.comms.RemoteServerDetector;
import celtech.roboxbase.comms.remote.Configuration;
import celtech.roboxbase.configuration.CoreMemory;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebView;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.input.ContextMenuEvent;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class RootScannerPanelController implements Initializable, MenuInnerPanel, DeviceDetectionListener
{

    private static final Stenographer steno = StenographerFactory.getStenographer(RootScannerPanelController.class.getName());

    private final RemoteServerDetector remoteServerDetector = RemoteServerDetector.getInstance();

    @FXML
    private Button scanButton;

    @FXML
    private TableView<DetectedServer> scannedRoots;

    @FXML
    private WebView rootWebView;

    @FXML
    private TableColumn nameColumn;

    @FXML
    private TableColumn pinColumn;

    @FXML
    private TableColumn statusColumn;

    private ObservableList<DetectedServer> currentServers = FXCollections.observableArrayList();

    private ContextMenu scannedRootContextMenu = new ContextMenu();

//    private static final class DetectedServerCell extends TableCell<DetectedServer, DetectedServer>
//    {
//
//        private DetectedServer lastServer = null;
//        private ChangeListener<Boolean> changeListener;
//
//        @Override
//        protected void updateItem(DetectedServer item, boolean empty)
//        {
//            if (lastServer != null)
//            {
//                lastServer.dataChangedProperty().removeListener(changeListener);
//            }
//            
//            if (!empty)
//            {
//                changeListener = new ChangeListener<Boolean>()
//                {
//                    @Override
//                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
//                    {
//                        
//                    }
//                };
//            }
//        }
//
//    }

//    class ServerListCell extends TableListCell<DetectedServer>
//    {
//
//        private final BorderPane cellContainer;
//        private final Label label;
//        private final StackPane buttonContainer;
//        private Circle statusBlob;
//        private final Button connectButton;
//        private final Button disconnectButton;
//        private final Button upgradeButton;
//        private DetectedServer detectedServer;
//
////        private final ChangeListener<ServerStatus> statusChangeListener = (ObservableValue<? extends ServerStatus> ov, ServerStatus t, ServerStatus t1) ->
////        {
////            switch (t1)
////            {
////                case NOT_THERE:
////                    statusBlob.setFill(Color.RED);
////                    break;
////                case OK:
////                    statusBlob.setFill(Color.GREEN);
////                    break;
////                case UNKNOWN:
////                    statusBlob.setFill(Color.YELLOW);
////                    break;
////                case WRONG_VERSION:
////                    statusBlob.setFill(Color.BLACK);
////                    break;
////            }
////        };
//
//        public ServerListCell()
//        {
//            cellContainer = new BorderPane();
//            cellContainer.setPrefWidth(USE_COMPUTED_SIZE);
//
////            HBox statusBlobHolder = new HBox();
////            statusBlobHolder.alignmentProperty().set(Pos.CENTER);
////            statusBlob = new Circle(10);
////            statusBlob.setFill(Color.YELLOW);
////            statusBlobHolder.getChildren().add(statusBlob);
////            cellContainer.leftProperty().set(statusBlobHolder);
//
//            label = new Label();
//            cellContainer.centerProperty().set(label);
//
//            connectButton = new Button("Connect");
//            connectButton.setOnAction((ActionEvent t) ->
//            {
//                if (detectedServer != null)
//                {
//                    detectedServer.connect();
//                }
//            });
//
//            disconnectButton = new Button("Disconnect");
//            disconnectButton.setVisible(false);
//            disconnectButton.setOnAction((ActionEvent t) ->
//            {
//                if (detectedServer != null)
//                {
//                    detectedServer.disconnect();
//                }
//            });
//
//            upgradeButton = new Button("Upgrade");
//            upgradeButton.setVisible(false);
//            upgradeButton.setOnAction((ActionEvent t) ->
//            {
//                if (detectedServer != null)
//                {
//                }
//            });
//
//            buttonContainer = new StackPane();
//            buttonContainer.getChildren().addAll(connectButton, disconnectButton);
//
//            cellContainer.rightProperty().set(buttonContainer);
//        }
//
//        @Override
//        protected void updateItem(DetectedServer item, boolean empty)
//        {
//            DetectedServer oldItem = getItem();
//
////            if (oldItem != null)
////            {
////                oldItem.getServerStatusProperty().removeListener(statusChangeListener);
////            }
//
//            super.updateItem(item, empty);
//
//            if (item != null && !empty)
//            {
//                detectedServer = item;
//                setGraphic(cellContainer);
//                label.setText(detectedServer.getName());
//
////                item.getServerStatusProperty().addListener(statusChangeListener);
//
//                connectButton.visibleProperty().unbind();
//                connectButton.visibleProperty().bind(item.getServerStatusProperty().isNotEqualTo(ServerStatus.OK)
//                        .and(item.getServerStatusProperty().isNotEqualTo(ServerStatus.WRONG_VERSION)));
//
//                disconnectButton.visibleProperty().unbind();
//                disconnectButton.visibleProperty().bind(detectedServer.getServerStatusProperty().isEqualTo(ServerStatus.OK));
//            } else
//            {
//                setGraphic(null);
//                label.setText("");
//            }
//            detectedServer = item;
//        }
//    }
    private void openWebViewOnRoot(DetectedServer server)
    {
        String url = "http://" + server.getAddress().getHostAddress() + ":" + Configuration.remotePort + "/";

        rootWebView.getEngine().load(url);
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
//        scannedRoots.setCellFactory(
//                (ListView<DetectedServer> param) -> new ServerListCell());

        nameColumn.setCellValueFactory(new PropertyValueFactory<DetectedServer, String>("name"));
        pinColumn.setCellValueFactory(new PropertyValueFactory<DetectedServer, String>("pin"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<DetectedServer, ServerStatus>("serverStatus"));

        scannedRoots.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        scannedRoots.setItems(currentServers);

        scannedRoots.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<DetectedServer>()
        {
            @Override
            public void changed(ObservableValue<? extends DetectedServer> observable, DetectedServer oldValue, DetectedServer newValue)
            {
                if (newValue.getServerStatus() == ServerStatus.OK)
                {
                    openWebViewOnRoot(newValue);
                }
            }
        });

        String connectText = "Connect";
        String disconnectText = "Disconnect";

        MenuItem connectItem = new MenuItem(connectText);
        MenuItem disconnectItem = new MenuItem(disconnectText);

        connectItem.setOnAction((ActionEvent e) ->
        {
            scannedRoots.getSelectionModel().getSelectedItem().connect();
        });
        disconnectItem.setOnAction((ActionEvent e) ->
        {
            scannedRoots.getSelectionModel().getSelectedItem().disconnect();
        });

        scannedRootContextMenu.getItems().add(connectItem);
        scannedRootContextMenu.getItems().add(disconnectItem);

        scannedRoots.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>()
        {

            @Override
            public void handle(ContextMenuEvent event)
            {
                scannedRootContextMenu.show(scannedRoots, event.getScreenX(), event.getScreenY());
            }
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
