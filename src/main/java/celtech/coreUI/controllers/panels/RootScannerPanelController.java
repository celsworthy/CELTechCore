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
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TextField;
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
    
    private TableColumn nameColumn;
    private TableColumn ipAddressColumn;
    private TableColumn versionColumn;
    private TableColumn<DetectedServer, ServerStatus> statusColumn;
    private TableColumn<DetectedServer, DetectedServer> scannedRootButtonsColumn;
    
    @FXML
    private TextField ipTextField;
    
    @FXML
    private Button addRootButton;
    
    @FXML
    private Button deleteRootButton;
    
    private static final String IPADDRESS_PATTERN
            = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    
    @FXML
    private void manuallyAddRoot(ActionEvent event)
    {
        String enteredIP = ipTextField.getText();
        try
        {
            DetectedServer newServer = new DetectedServer(InetAddress.getByName(enteredIP));
            newServer.setWasAutomaticallyAdded(false);
            checkAndAddServer(newServer);
            ipTextField.setText("");
        } catch (UnknownHostException ex)
        {
            steno.error("Bad IP address for manually added Root: " + enteredIP);
        }
    }
    
    @FXML
    private void manuallyDeleteRoot(ActionEvent event)
    {
        String enteredIP = ipTextField.getText();
        
        DetectedServer matchingServer = null;
        for (DetectedServer server : currentServers)
        {
            if (server.getAddress().getHostAddress().equals(enteredIP))
            {
                matchingServer = server;
                break;
            }
        }
        
        if (matchingServer != null)
        {
            matchingServer.disconnect();
            currentServers.remove(matchingServer);
        }
    }
    
    private final ObservableList<DetectedServer> currentServers = FXCollections.observableArrayList();
    
    private void checkAndAddServer(DetectedServer server)
    {
        if (!server.whoAreYou())
        {
            CoreMemory.getInstance().deactivateRoboxRoot(server);
        } else
        {
            server.connect();
            Platform.runLater(() ->
            {
                currentServers.add(server);
            });
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
        versionColumn.setPrefWidth(100);
        versionColumn.setResizable(false);
        versionColumn.setStyle("-fx-alignment: CENTER;");
        
        statusColumn = new TableColumn<>();
        statusColumn.setCellFactory(statusCell -> new RootTableCell());
        statusColumn.setCellValueFactory(new PropertyValueFactory<DetectedServer, ServerStatus>("serverStatus"));
        statusColumn.setPrefWidth(40);
        statusColumn.setResizable(false);
        
        scannedRootButtonsColumn = new TableColumn<>();
        scannedRootButtonsColumn.setCellFactory(buttonCell -> new RootConnectionButtonTableCell());
        scannedRootButtonsColumn.setCellValueFactory((CellDataFeatures<DetectedServer, DetectedServer> p) -> new SimpleObjectProperty<>(p.getValue()));
        scannedRootButtonsColumn.setMinWidth(350);
        scannedRootButtonsColumn.setMaxWidth(Integer.MAX_VALUE);
        scannedRootButtonsColumn.setResizable(false);
        
        scannedRoots.getColumns().add(nameColumn);
        scannedRoots.getColumns().add(ipAddressColumn);
        scannedRoots.getColumns().add(versionColumn);
        scannedRoots.getColumns().add(statusColumn);
        scannedRoots.getColumns().add(scannedRootButtonsColumn);
        scannedRoots.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        HBox.setHgrow(scannedRoots, Priority.ALWAYS);
        
        scannedRoots.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        scannedRoots.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<DetectedServer>()
        {
            @Override
            public void changed(ObservableValue<? extends DetectedServer> observable, DetectedServer oldValue, DetectedServer newValue)
            {
                if (newValue == null
                        || newValue.getWasAutomaticallyAdded())
                {
                    ipTextField.setText("");
                } else
                {
                    ipTextField.setText(newValue.getServerIP());
                }
            }
        });
        
        scannedRoots.setItems(currentServers);
        
        scannedRoots.setPlaceholder(new Text(BaseLookup.i18n("rootScanner.noRemoteServersFound")));
        
        ipTextField.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                String enteredIP = ipTextField.getText();
                if (enteredIP.matches(IPADDRESS_PATTERN))
                {
                    DetectedServer matchingServer = null;
                    for (DetectedServer server : currentServers)
                    {
                        if (server.getAddress().getHostAddress().equals(enteredIP))
                        {
                            matchingServer = server;
                            break;
                        }
                    }
                    
                    if (matchingServer != null
                            && !matchingServer.getWasAutomaticallyAdded())
                    {
                        addRootButton.setDisable(true);
                        deleteRootButton.setDisable(false);
                    } else if (matchingServer == null)
                    {
                        // Allow the IP to be added - it is not in the list
                        addRootButton.setDisable(false);
                        deleteRootButton.setDisable(true);
                    } else
                    {
                        // The entered IP is that of an automatically added server
                        addRootButton.setDisable(false);
                        deleteRootButton.setDisable(true);
                    }
                } else
                {
                    addRootButton.setDisable(true);
                    deleteRootButton.setDisable(true);
                }
            }
        });
        
        addRootButton.setDisable(true);
        deleteRootButton.setDisable(true);
        
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
                    checkAndAddServer(server);
                    currentServerList.add(server);
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
                            } else
                            {
                                //Need to update an existing server
                                DetectedServer serverInList = currentServerList.get(currentServerList.indexOf(server));
                                if (!serverInList.getName().equals(server.getName()))
                                {
                                    serverInList.setName(server.getName());
                                }
                                if (!serverInList.getPin().equals(server.getPin()))
                                {
                                    serverInList.setPin(server.getPin());
                                }
                            }
                        }
                        
                        for (DetectedServer server : currentServerList)
                        {
                            if (!foundServers.contains(server)
                                    && server.getWasAutomaticallyAdded())
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
