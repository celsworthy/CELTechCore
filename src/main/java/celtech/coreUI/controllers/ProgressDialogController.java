/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.services.ControllableService;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author ianhudson
 */
public class ProgressDialogController implements Initializable
{
    
    private Stenographer steno = StenographerFactory.getStenographer(ProgressDialogController.class.getName());
    @FXML
    private StackPane progressDialog;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressTitle;
    @FXML
    private Label progressMessage;
    @FXML
    private Label progressPercent;
    @FXML
    private Button progressCancel;

    /*
     * 
     */
    private ControllableService serviceBeingMonitored = null;
    private ChangeListener<Boolean> registeredListener = null;
    
    /**
     *
     * @param event
     */
    public void cancelOperation(MouseEvent event)
    {
        serviceBeingMonitored.cancelRun();
    }
    
    /**
     *
     * @param service
     * @param stage
     */
    public void configure(ControllableService service, final Stage stage)
    {
        serviceBeingMonitored = service;
        progressTitle.textProperty().unbind();
        progressTitle.textProperty().bind(serviceBeingMonitored.titleProperty());
        progressMessage.textProperty().unbind();
        progressMessage.textProperty().bind(serviceBeingMonitored.messageProperty());
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(serviceBeingMonitored.progressProperty());
        progressPercent.textProperty().unbind();
        progressPercent.textProperty().bind(serviceBeingMonitored.progressProperty().multiply(100f).asString("%.0f%%"));
        
        if (registeredListener != null)
        {
            serviceBeingMonitored.runningProperty().removeListener(registeredListener);
            registeredListener = null;
        }
        
        ChangeListener<Boolean> serviceRunningListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue)
            {
                if (newValue == true)
                {
                    stage.show();
//                    rebind();
                } else
                {
                    stage.close();
                }
            }
        };
        
        serviceBeingMonitored.runningProperty().addListener(serviceRunningListener);
        
        registeredListener = serviceRunningListener;
    }
    
    private void rebind()
    {
        /*
         * Unbind everything...
         */
        progressTitle.textProperty().unbind();
        progressMessage.textProperty().unbind();
        progressBar.progressProperty().unbind();
        progressPercent.textProperty().unbind();
        //
        /*
         * Bind/rebind
         */
        progressTitle.textProperty().bind(serviceBeingMonitored.titleProperty());
        progressMessage.textProperty().bind(serviceBeingMonitored.messageProperty());
        progressBar.progressProperty().bind(serviceBeingMonitored.progressProperty());
        progressPercent.textProperty().bind(serviceBeingMonitored.progressProperty().multiply(100f).asString("%.0f%%"));
    }

    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        //progressDialog.setVisible(false);
        progressPercent.setText("");
        progressTitle.setText("");
        progressMessage.setText("");
        progressBar.setProgress(0f);
        
    }
}
