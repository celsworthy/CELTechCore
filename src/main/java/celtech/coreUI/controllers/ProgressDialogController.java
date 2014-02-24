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
    
    public void cancelOperation(MouseEvent event)
    {
        serviceBeingMonitored.cancelRun();
    }
    
    public void configure(ControllableService service, final Stage stage)
    {
        serviceBeingMonitored = service;
        progressTitle.textProperty().bind(serviceBeingMonitored.titleProperty());
        progressMessage.textProperty().bind(serviceBeingMonitored.messageProperty());
        progressBar.progressProperty().bind(serviceBeingMonitored.progressProperty());
        progressPercent.textProperty().bind(serviceBeingMonitored.progressProperty().multiply(100f).asString("%.0f%%"));
        
        serviceBeingMonitored.runningProperty().addListener(new ChangeListener<Boolean>()
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
        });
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
