package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import celtech.appManager.ShapeContainerProject;
import celtech.appManager.StylusSettings;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class StylusSettingsInsetPanelController implements Initializable, ProjectAwareController
{

    private final Stenographer steno = StenographerFactory.getStenographer(StylusSettingsInsetPanelController.class.getName());
    
    @FXML
    private HBox stylusSettingsInsetRoot;
    
    @FXML
    private GridPane stylusSettingsGridPane;
    
    @FXML
    private Label headTypeLabel;
    @FXML
    private Label dragKnifeLabel;
    @FXML
    private Label dragKnifeRadiusLabel;
    @FXML
    private Label xOffsetLabel;
    @FXML
    private Label yOffsetLabel;
    @FXML
    private Label zOffsetLabel;
    @FXML
    private CheckBox dragKnifeCheckbox;
    @FXML
    private RestrictedNumberField dragKnifeRadiusEntry;
    @FXML
    private RestrictedNumberField xOffsetEntry;
    @FXML
    private RestrictedNumberField yOffsetEntry;
    @FXML
    private RestrictedNumberField zOffsetEntry;
    
    private Project currentProject;
    private Printer currentPrinter;
    private String currentHeadType = "";
    private final SimpleBooleanProperty headTypeIsStylus = new SimpleBooleanProperty(false);
    private boolean suppressUpdates = false;
    
    private final ChangeListener<Boolean> gCodePrepChangeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        currentPrinter = Lookup.getSelectedPrinterProperty().get();
        updateHeadType(currentPrinter);        
        updateFields(currentProject);
    };

    private final ChangeListener<ApplicationMode> applicationModeChangeListener = new ChangeListener<ApplicationMode>()
    {
        @Override
        public void changed(ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue)
        {
            if (newValue == ApplicationMode.SETTINGS &&
                currentProject != null &&
                currentProject.getMode() == ProjectMode.SVG)
            {
                stylusSettingsInsetRoot.setVisible(true);
                stylusSettingsInsetRoot.setMouseTransparent(false);
                if (Lookup.getSelectedProjectProperty().get() == currentProject)
                {
                    updateFields(currentProject);
                }
            } else
            {
                stylusSettingsInsetRoot.setVisible(false);
                stylusSettingsInsetRoot.setMouseTransparent(true);
            }
        }
    };

    /**
     * Initialises the controller class.
     * 
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            currentPrinter = Lookup.getSelectedPrinterProperty().get();
            updateHeadType(Lookup.getSelectedPrinterProperty().get());

            ApplicationStatus.getInstance()
                    .modeProperty().addListener(applicationModeChangeListener);
        } catch (Exception ex)
        {
            steno.exception("Exception when initializing TimeCostInsetPanel", ex);
        }

        //stylusSettingsGridPane.disableProperty().bind(headTypeIsStylus.not());
        dragKnifeRadiusEntry.disableProperty().bind(dragKnifeCheckbox.selectedProperty().not());
        dragKnifeRadiusLabel.disableProperty().bind(dragKnifeCheckbox.selectedProperty().not());
        
        dragKnifeCheckbox.selectedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean selected) -> {
            if (currentProject != null)
                doActionSuppressedUpdates(() -> ((ShapeContainerProject)currentProject).getStylusSettings().setHasDragKnife(selected));
        });
        
        dragKnifeRadiusEntry.valueChangedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) -> {
            double newValue = dragKnifeRadiusEntry.getAsDouble();
            if (currentProject != null)
                doActionSuppressedUpdates(() -> ((ShapeContainerProject)currentProject).getStylusSettings().setDragKnifeRadius(newValue));
        });
        
        xOffsetEntry.valueChangedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) -> {
            double newValue = xOffsetEntry.getAsDouble();
            if (currentProject != null)
                doActionSuppressedUpdates(() -> ((ShapeContainerProject)currentProject).getStylusSettings().setXOffset(newValue));
        });
        
        yOffsetEntry.valueChangedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) -> {
            double newValue = yOffsetEntry.getAsDouble();
            if (currentProject != null)
                doActionSuppressedUpdates(() -> ((ShapeContainerProject)currentProject).getStylusSettings().setYOffset(newValue));
        });
        
        zOffsetEntry.valueChangedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) -> {
            double newValue = zOffsetEntry.getAsDouble();
            if (currentProject != null) 
                doActionSuppressedUpdates(() -> ((ShapeContainerProject)currentProject).getStylusSettings().setZOffset(newValue));
        });
    }
    
    private void updateHeadType(Printer printer)
    {
        String headTypeBefore = currentHeadType;
        Head.HeadType headEType = HeadContainer.defaultHeadType;
        
        if (printer != null && printer.headProperty().get() != null)
        {
            currentHeadType = printer.headProperty().get().typeCodeProperty().get();
            headEType = printer.headProperty().get().headTypeProperty().get();
        } else
        {
            currentHeadType = HeadContainer.defaultHeadID;
        }
        if (!headTypeBefore.equals(currentHeadType))
        {
            headTypeIsStylus.set(headEType == Head.HeadType.STYLUS_HEAD);
            BaseLookup.getTaskExecutor().runOnGUIThread(() ->
            {
                headTypeLabel.setText(currentHeadType);
            });
        }
    }
    
    @Override
    public void setProject(Project project)
    {
        if (currentProject != null)
            currentProject.getGCodeGenManager().getDataChangedProperty().removeListener(this.gCodePrepChangeListener);
        
        currentProject = project;

        if (currentProject != null) {
            currentProject.getGCodeGenManager().getDataChangedProperty().addListener(this.gCodePrepChangeListener);
        }
    }
    
    private void updateFields(Project project)
    {
        if (!suppressUpdates)
        {
            ShapeContainerProject stylusProject = (ShapeContainerProject)project;
            StylusSettings stylusSettings = stylusProject.getStylusSettings();

            dragKnifeCheckbox.selectedProperty().set(stylusSettings.getHasDragKnife());
            dragKnifeRadiusEntry.setValue(stylusSettings.getDragKnifeRadius());
            xOffsetEntry.setValue(stylusSettings.getXOffset());
            yOffsetEntry.setValue(stylusSettings.getYOffset());
            zOffsetEntry.setValue(stylusSettings.getZOffset());
        }
    }
    
    private void doActionSuppressedUpdates(Runnable action)
    {
        try {
            suppressUpdates = true;
            action.run();
        }
        finally {
            suppressUpdates = false;
        }
    }
    
    @Override
    public void shutdownController()
    {

        if (currentProject != null)
            currentProject.getGCodeGenManager().getDataChangedProperty().removeListener(this.gCodePrepChangeListener);
        currentProject = null;

        ApplicationStatus.getInstance()
                .modeProperty().removeListener(applicationModeChangeListener);
    }
}
