package celtech.coreUI.controllers.panels;

import celtech.roboxbase.comms.DetectedDevice;
import celtech.roboxbase.comms.DetectedServer;
import celtech.roboxbase.comms.DetectedServer.ServerStatus;
import celtech.roboxbase.comms.DeviceDetectionListener;
import celtech.roboxbase.comms.RemoteServerDetector;
import celtech.roboxbase.configuration.CoreMemory;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.glyphfont.Glyph;

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
    private ListView<DetectedServer> scannedRoots;

    @FXML
    void scanForRoots(ActionEvent event)
    {
        List<DetectedServer> foundServers = remoteServerDetector.searchForServers();
        scannedRoots.setItems(FXCollections.observableArrayList(foundServers));
    }

    class ServerListCell extends ListCell<DetectedServer>
    {

        private final BorderPane cellContainer;
        private final Label label;
        private final StackPane buttonContainer;
        private Circle statusBlob;
        private final Button connectButton;
        private final Button disconnectButton;
        private DetectedServer detectedServer;

        private final ChangeListener<ServerStatus> statusChangeListener = (ObservableValue<? extends ServerStatus> ov, ServerStatus t, ServerStatus t1) ->
        {
            switch (t1)
            {
                case NOT_THERE:
                    statusBlob.setFill(Color.RED);
                    break;
                case OK:
                    statusBlob.setFill(Color.GREEN);
                    break;
                case UNKNOWN:
                    statusBlob.setFill(Color.YELLOW);
                    break;
                case WRONG_VERSION:
                    statusBlob.setFill(Color.BLACK);
                    break;
            }
        };

        public ServerListCell()
        {
            cellContainer = new BorderPane();
            cellContainer.setPrefWidth(USE_COMPUTED_SIZE);

            HBox statusBlobHolder = new HBox();
            statusBlobHolder.alignmentProperty().set(Pos.CENTER);
            statusBlob = new Circle(10);
            statusBlob.setFill(Color.YELLOW);
            statusBlobHolder.getChildren().add(statusBlob);
            cellContainer.leftProperty().set(statusBlobHolder);

            label = new Label();
            cellContainer.centerProperty().set(label);

            connectButton = new Button("Connect");
            connectButton.setOnAction((ActionEvent t) ->
            {
                if (detectedServer != null)
                {
                    detectedServer.connect();
                }
            });
            
            disconnectButton = new Button("Disconnect");
            disconnectButton.setVisible(false);
            disconnectButton.setOnAction((ActionEvent t) ->
            {
                if (detectedServer != null)
                {
                    detectedServer.disconnect();
                }
            });
            
            buttonContainer = new StackPane();
            buttonContainer.getChildren().addAll(connectButton, disconnectButton);

            cellContainer.rightProperty().set(buttonContainer);
        }

        @Override
        protected void updateItem(DetectedServer item, boolean empty)
        {
            DetectedServer oldItem = getItem();

            if (oldItem != null)
            {
                oldItem.getServerStatusProperty().removeListener(statusChangeListener);
            }

            super.updateItem(item, empty);

            if (item != null && !empty)
            {
                detectedServer = item;
                setGraphic(cellContainer);
                label.setText(detectedServer.getName() + "@" + detectedServer.getAddress().getHostAddress() + " v" + detectedServer.getVersion());

                item.getServerStatusProperty().addListener(statusChangeListener);

                connectButton.visibleProperty().unbind();
                connectButton.visibleProperty().bind(item.getServerStatusProperty().isNotEqualTo(ServerStatus.OK)
                        .and(item.getServerStatusProperty().isNotEqualTo(ServerStatus.WRONG_VERSION)));

                disconnectButton.visibleProperty().unbind();
                disconnectButton.visibleProperty().bind(detectedServer.getServerStatusProperty().isEqualTo(ServerStatus.OK));
            } else
            {
                setGraphic(null);
                label.setText("");
            }
            detectedServer = item;
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
        scannedRoots.setCellFactory(
                (ListView<DetectedServer> param) -> new ServerListCell());

        scannedRoots.setItems(FXCollections.observableArrayList(CoreMemory.getInstance().getActiveRoboxRoots()));
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
