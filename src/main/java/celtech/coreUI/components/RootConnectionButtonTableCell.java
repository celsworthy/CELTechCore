package celtech.coreUI.components;

import celtech.Lookup;
import celtech.coreUI.components.Notifications.GenericProgressBar;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.comms.DetectedServer;
import celtech.roboxbase.comms.DetectedServer.ServerStatus;
import celtech.roboxbase.comms.remote.Configuration;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.MachineType;
import celtech.roboxbase.utils.SystemUtils;
import celtech.utils.TaskWithProgessCallback;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 *
 * @author Ian
 */
public class RootConnectionButtonTableCell extends TableCell<DetectedServer, DetectedServer>
{

    private GenericProgressBar rootSoftwareUpDownloadProgress;

    private BooleanProperty inhibitUpdate = new SimpleBooleanProperty(false);

    @FXML
    private HBox connectedBox;

    @FXML
    private HBox disconnectedBox;

    @FXML
    private TextField pinEntryField;

    @FXML
    private Button updateButton;

    @FXML
    void connectToServer(ActionEvent event)
    {
        if (associatedServer != null)
        {
            associatedServer.setPin(pinEntryField.getText());
            associatedServer.connect();
        }
    }

    @FXML
    void disconnectFromServer(ActionEvent event)
    {
        if (associatedServer != null)
        {
            associatedServer.disconnect();
        }
    }

    @FXML
    void deleteServer(ActionEvent event)
    {
        if (associatedServer != null)
        {
            associatedServer.disconnect();
        }
    }

    private void upgradeRootWithFile(String path, String filename)
    {
        if (associatedServer != null)
        {
            TaskWithProgessCallback<Boolean> rootUploader = new TaskWithProgessCallback<Boolean>()
            {
                @Override
                protected Boolean call() throws Exception
                {
                    return associatedServer.upgradeRootSoftware(path, filename, this);
                }

                @Override
                public void updateProgressPercent(double percentProgress)
                {
                    updateProgress(percentProgress, 100.0);
                }
            };

            rootUploader.setOnScheduled((event) ->
            {
                inhibitUpdate.set(true);
            });

            rootUploader.setOnFailed((event) ->
            {
                BaseLookup.getSystemNotificationHandler().showErrorNotification(Lookup.i18n("rootScanner.rootUploadTitle"), Lookup.i18n("rootScanner.failedUploadMessage"));
                Lookup.getProgressDisplay().removeGenericProgressBarFromDisplay(rootSoftwareUpDownloadProgress);
                rootSoftwareUpDownloadProgress = null;
                inhibitUpdate.set(false);
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
                inhibitUpdate.set(false);
            });

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

    @FXML
    void updateRoot(ActionEvent event)
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

            rootDownloader.setOnScheduled((result) ->
            {
                inhibitUpdate.set(true);
            });

            rootDownloader.setOnSucceeded((result) ->
            {
                BaseLookup.getSystemNotificationHandler().showErrorNotification(Lookup.i18n("rootScanner.rootDownloadTitle"), Lookup.i18n("rootScanner.successfulDownloadMessage"));
                upgradeRootWithFile(pathToRootFile, rootFile);
                Lookup.getProgressDisplay().removeGenericProgressBarFromDisplay(rootSoftwareUpDownloadProgress);
                rootSoftwareUpDownloadProgress = null;
                inhibitUpdate.set(false);
            });

            rootDownloader.setOnFailed((result) ->
            {
                BaseLookup.getSystemNotificationHandler().showErrorNotification(Lookup.i18n("rootScanner.rootDownloadTitle"), Lookup.i18n("rootScanner.failedDownloadMessage"));
                Lookup.getProgressDisplay().removeGenericProgressBarFromDisplay(rootSoftwareUpDownloadProgress);
                rootSoftwareUpDownloadProgress = null;
                inhibitUpdate.set(false);
            });

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
    private void launchRootManager(ActionEvent event)
    {
        String url = "http://" + associatedServer.getServerIP() + ":" + Configuration.remotePort + "/index.html";
        if (Desktop.isDesktopSupported()
                && BaseConfiguration.getMachineType()
                != MachineType.LINUX_X86
                && BaseConfiguration.getMachineType()
                != MachineType.LINUX_X64)
        {
            try
            {
                URI linkToVisit = new URI(url);
                Desktop.getDesktop().browse(linkToVisit);
            } catch (IOException | URISyntaxException ex)
            {
                System.err.println("Error when attempting to browse to "
                        + url);
            }
        } else if (BaseConfiguration.getMachineType() == MachineType.LINUX_X86
                || BaseConfiguration.getMachineType() == MachineType.LINUX_X64)
        {
            try
            {
                if (Runtime.getRuntime().exec(new String[]
                {
                    "which", "xdg-open"
                }).getInputStream().read() != -1)
                {
                    Runtime.getRuntime().exec(new String[]
                    {
                        "xdg-open", url
                    });
                }
            } catch (IOException ex)
            {
                System.err.println("Failed to run linux-specific browser command");
            }
        } else
        {
            System.err.println(
                    "Couldn't get Desktop - not able to support hyperlinks");
        }
    }

    private StackPane buttonHolder;
    private DetectedServer associatedServer = null;
    private ChangeListener<ServerStatus> serverStatusListener = new ChangeListener<ServerStatus>()
    {
        @Override
        public void changed(ObservableValue<? extends ServerStatus> observable, ServerStatus oldValue, ServerStatus newValue)
        {
            processServerStatus(newValue);
        }
    };

    public RootConnectionButtonTableCell()
    {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "/celtech/resources/fxml/components/RootConnectionButtonTableCell.fxml"),
                BaseLookup.getLanguageBundle());
        fxmlLoader.setController(this);

        fxmlLoader.setClassLoader(this.getClass().getClassLoader());

        try
        {
            buttonHolder = fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }

        updateButton.disableProperty().bind(inhibitUpdate);
    }

    @Override
    protected void updateItem(DetectedServer item, boolean empty)
    {
        super.updateItem(item, empty);

        if (item != associatedServer)
        {
            if (associatedServer != null)
            {
                associatedServer.serverStatusProperty().removeListener(serverStatusListener);
            }
            if (item != null)
            {
                item.serverStatusProperty().addListener(serverStatusListener);
            }
            associatedServer = item;
        }

        if (item != null && !empty)
        {
            setGraphic(buttonHolder);
            processServerStatus(item.getServerStatus());
        } else
        {
            setGraphic(null);
        }
    }

    private void processServerStatus(ServerStatus status)
    {
        switch (status)
        {
            case CONNECTED:
                connectedBox.setVisible(true);
                disconnectedBox.setVisible(false);
                updateButton.setVisible(false);
                break;
            case NOT_CONNECTED:
            case WRONG_PIN:
                disconnectedBox.setVisible(true);
                connectedBox.setVisible(false);
                updateButton.setVisible(false);
                break;
            case WRONG_VERSION:
                updateButton.setVisible(true);
                disconnectedBox.setVisible(false);
                connectedBox.setVisible(false);
                break;
            default:
                disconnectedBox.setVisible(false);
                connectedBox.setVisible(false);
                updateButton.setVisible(false);
                break;
        }
    }
}
