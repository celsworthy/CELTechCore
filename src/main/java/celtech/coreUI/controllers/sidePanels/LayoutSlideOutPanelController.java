/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.sidePanels;

import celtech.appManager.Project;
import celtech.printerControl.PrintJob;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

/**
 *
 * @author Ian
 */
public class LayoutSlideOutPanelController implements Initializable
{

    private Project currentProject = null;

    @FXML
    private Label lastModifiedDate;

    @FXML
    private ListView printHistory;

    private final ListChangeListener<PrintJob> printJobChangeListener = new ListChangeListener<PrintJob>()
    {
        @Override
        public void onChanged(ListChangeListener.Change<? extends PrintJob> c)
        {
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
    }

    public void bindLoadedModels(final Project currentProject)
    {
        if (this.currentProject != null)
        {
            this.currentProject.getPrintJobs().removeListener(printJobChangeListener);
        }

        this.currentProject = currentProject;
        lastModifiedDate.textProperty().unbind();
        lastModifiedDate.textProperty().bind(currentProject.getProjectHeader().getLastModifiedDateProperty().asString());

        printHistory.getItems().clear();

        currentProject.getPrintJobs().addListener(printJobChangeListener);

    }
}
