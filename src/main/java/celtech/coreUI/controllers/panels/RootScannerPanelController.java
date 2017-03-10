package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.WebEngineFix.AMURLStreamHandlerFactory;
import celtech.coreUI.components.RootConnectionButtonTableCell;
import celtech.coreUI.components.RootTableCell;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.comms.DetectedDevice;
import celtech.roboxbase.comms.DetectedServer;
import celtech.roboxbase.comms.DetectedServer.ServerStatus;
import celtech.roboxbase.comms.DeviceDetectionListener;
import celtech.roboxbase.comms.RemoteServerDetector;
import celtech.roboxbase.configuration.CoreMemory;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import javafx.concurrent.Task;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;

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
    private TableView<DetectedServer> scannedRoots;

    @FXML
    private TableColumn nameColumn;

    @FXML
    private TableColumn ipAddressColumn;

    @FXML
    private TableColumn versionColumn;

    @FXML
    private TableColumn<DetectedServer, ServerStatus> statusColumn;

    @FXML
    private TableColumn<DetectedServer, DetectedServer> buttonsColumn;

    private final ObservableList<DetectedServer> currentServers = FXCollections.observableArrayList();

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
        versionColumn.setPrefWidth(100);
        versionColumn.setResizable(false);
        versionColumn.setStyle("-fx-alignment: CENTER;");

        statusColumn = new TableColumn<>();
        statusColumn.setCellFactory(statusCell -> new RootTableCell());
        statusColumn.setCellValueFactory(new PropertyValueFactory<DetectedServer, ServerStatus>("serverStatus"));
        statusColumn.setPrefWidth(40);
        statusColumn.setResizable(false);

        buttonsColumn = new TableColumn<>();
        buttonsColumn.setCellFactory(buttonCell -> new RootConnectionButtonTableCell());
        buttonsColumn.setCellValueFactory((CellDataFeatures<DetectedServer, DetectedServer> p) -> new SimpleObjectProperty<>(p.getValue()));
        buttonsColumn.setMinWidth(350);
        buttonsColumn.setMaxWidth(Integer.MAX_VALUE);
        buttonsColumn.setResizable(false);

        scannedRoots.getColumns().add(nameColumn);
        scannedRoots.getColumns().add(ipAddressColumn);
        scannedRoots.getColumns().add(versionColumn);
        scannedRoots.getColumns().add(statusColumn);
        scannedRoots.getColumns().add(buttonsColumn);
        scannedRoots.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        HBox.setHgrow(scannedRoots, Priority.ALWAYS);

        scannedRoots.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        scannedRoots.setItems(currentServers);

        scannedRoots.setPlaceholder(new Text(BaseLookup.i18n("rootScanner.noRemoteServersFound")));

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
                    try
                    {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex)
                    {
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
