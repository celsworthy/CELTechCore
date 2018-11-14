package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.GCodeGeneratorManager;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.RoboxProfile;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import celtech.roboxbase.utils.tasks.Cancellable;
import celtech.roboxbase.utils.tasks.SimpleCancellable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class TimeCostInsetPanelController implements Initializable, ProjectAwareController
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            TimeCostInsetPanelController.class.getName());
    
    @FXML
    private HBox timeCostInsetRoot;

    @FXML
    private Label lblDraftTime;
    @FXML
    private Label lblNormalTime;
    @FXML
    private Label lblFineTime;
    @FXML
    private Label lblCustomTime;
    @FXML
    private Label lblDraftWeight;
    @FXML
    private Label lblNormalWeight;
    @FXML
    private Label lblFineWeight;
    @FXML
    private Label lblCustomWeight;
    @FXML
    private Label lblDraftCost;
    @FXML
    private Label lblNormalCost;
    @FXML
    private Label lblFineCost;
    @FXML
    private Label lblCustomCost;
    @FXML
    private RadioButton rbDraft;
    @FXML
    private RadioButton rbNormal;
    @FXML
    private RadioButton rbFine;
    @FXML
    private RadioButton rbCustom;
    @FXML
    private Label headType;

    private ToggleGroup qualityToggleGroup;

    private Project currentProject;
    private Printer currentPrinter;
    private String currentHeadType;

    private boolean settingPrintQuality = false;
    
    private List<PrintQualityEnumeration> sliceOrder = new ArrayList<>(Arrays.asList(PrintQualityEnumeration.NORMAL,
            PrintQualityEnumeration.DRAFT,
            PrintQualityEnumeration.FINE,
            PrintQualityEnumeration.CUSTOM));
    
    private final TimeCostThreadManager timeCostThreadManager = TimeCostThreadManager.getInstance();

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
            if (newValue == ApplicationMode.SETTINGS)
            {
                timeCostInsetRoot.setVisible(true);
                timeCostInsetRoot.setMouseTransparent(false);
                if (Lookup.getSelectedProjectProperty().get() == currentProject)
                {
                    updateFields(currentProject);
                }
            } else
            {
                timeCostInsetRoot.setVisible(false);
                timeCostInsetRoot.setMouseTransparent(true);
                timeCostThreadManager.cancelRunningTimeCostTasks();
            }
        }
    };

    /**
     * Initialises the controller class.
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

            setupQualityRadioButtons();
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void updateHeadType(Printer printer)
    {
        String headTypeBefore = currentHeadType;
        if (printer != null && printer.headProperty().get() != null)
        {
            currentHeadType = printer.headProperty().get().typeCodeProperty().get();
        } else
        {
            currentHeadType = HeadContainer.defaultHeadID;
        }
        if (headTypeBefore != currentHeadType)
        {
            BaseLookup.getTaskExecutor().runOnGUIThread(() ->
            {
                headType.setText("Estimates for head type: " + currentHeadType.toString());
            });
        }
    }

    private void setupQualityRadioButtons()
    {
        qualityToggleGroup = new ToggleGroup();
        rbDraft.setToggleGroup(qualityToggleGroup);
        rbDraft.setUserData(PrintQualityEnumeration.DRAFT);
        rbNormal.setToggleGroup(qualityToggleGroup);
        rbNormal.setUserData(PrintQualityEnumeration.NORMAL);
        rbFine.setToggleGroup(qualityToggleGroup);
        rbFine.setUserData(PrintQualityEnumeration.FINE);
        rbCustom.setToggleGroup(qualityToggleGroup);
        rbCustom.setUserData(PrintQualityEnumeration.CUSTOM);
        qualityToggleGroup.selectedToggleProperty().addListener(
                (ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) ->
                {
                    settingPrintQuality = true;
                    GCodeGeneratorManager gCodeGeneratorManager = ((ModelContainerProject) currentProject).getGCodeGenManager();
                    changeSlicingOrder((PrintQualityEnumeration) newValue.getUserData(), gCodeGeneratorManager);
                    
                    if (currentProject != null && currentProject instanceof ModelContainerProject) {
                        gCodeGeneratorManager.setSuppressReaction(true);
                    }
                    
                    currentProject.getPrinterSettings().setPrintQuality((PrintQualityEnumeration) newValue.getUserData());
                    
                    if (currentProject != null && currentProject instanceof ModelContainerProject) {
                        gCodeGeneratorManager.setSuppressReaction(false);
                    }
                    
                    settingPrintQuality = false;
                });
    }
    
    private void changeSlicingOrder(PrintQualityEnumeration firstToSlice, GCodeGeneratorManager gCodeGeneratorManager) {
        sliceOrder = new ArrayList<>(Arrays.asList(PrintQualityEnumeration.values()));
        sliceOrder.remove(firstToSlice);
        sliceOrder.add(0, firstToSlice);
        gCodeGeneratorManager.setSlicingOrder(sliceOrder);
    }

    @Override
    public void setProject(Project project)
    {
        if (currentProject != null && currentProject instanceof ModelContainerProject)
            ((ModelContainerProject)currentProject).getGCodeGenManager().getDataChangedProperty().removeListener(this.gCodePrepChangeListener);

        
        currentProject = project;
        if(currentProject != null) {
            selectPrintProfile(currentProject.getPrintQuality());
        }
        
        if (currentProject != null && currentProject instanceof ModelContainerProject)
            ((ModelContainerProject)currentProject).getGCodeGenManager().getDataChangedProperty().addListener(this.gCodePrepChangeListener);
    }
    
    private void selectPrintProfile(PrintQualityEnumeration printQuality) {
        switch(printQuality) {
            case DRAFT:
                rbDraft.setSelected(true);
                break;
            case NORMAL:
                rbNormal.setSelected(true);
                break;
            case FINE:
                rbFine.setSelected(true);
                break;
            case CUSTOM:
                rbCustom.setSelected(true);
                break;
        }
    }

    /**
     * Update the time, cost and weight fields. Long running calculations must
     * be performed in a background thread. Run draft, normal and fine
     * sequentially to avoid flooding the CPU(s).
     */
    private void updateFields(Project project)
    {

        if (settingPrintQuality || ApplicationStatus.getInstance().modeProperty().get() != ApplicationMode.SETTINGS)
        {
            return;
        }

        BaseLookup.getTaskExecutor().runOnGUIThread(() ->
        {
            lblDraftTime.setText("...");
            lblNormalTime.setText("...");
            lblFineTime.setText("...");
            lblCustomTime.setText("...");
            lblDraftWeight.setText("...");
            lblNormalWeight.setText("...");
            lblFineWeight.setText("...");
            lblCustomWeight.setText("...");
            lblDraftCost.setText("...");
            lblNormalCost.setText("...");
            lblFineCost.setText("...");
            lblCustomCost.setText("...");
        });

        Cancellable cancellable = new SimpleCancellable();
        
        String headTypeToUse = HeadContainer.defaultHeadID;
        if (currentPrinter != null
                && currentPrinter.headProperty().get() != null)
        {
            headTypeToUse = currentPrinter.headProperty().get().typeCodeProperty().get();
        }
        
        RoboxProfile profileSettings = null; // This is sometimes returned as null. Not sure why.
        if (currentProject != null && currentProject.getNumberOfProjectifiableElements() > 0)
        {
            profileSettings = currentProject.getPrinterSettings().getSettings(headTypeToUse, getSlicerType());
        }
        if (profileSettings == null)
        {
            steno.error("profileSettings == null!");
        }
        else
        {
            // NOTE - this needs to change if raft settings in slicermapping.dat is changed
            // Needed as heads differ in size and will need to adjust print volume for this
            double zReduction = 0.0;
            if (currentPrinter != null
                && currentPrinter.headProperty().get() != null)
            {
                zReduction = currentPrinter.headProperty().get().getZReductionProperty().get();
            }
            double raftOffset = profileSettings.getSpecificFloatSetting("raftBaseThickness_mm")
                    //Raft interface thickness
                    + 0.28
                    //Raft surface layer thickness * surface layers
                    + (profileSettings.getSpecificIntSetting("interfaceLayers")* 0.27)
                    + profileSettings.getSpecificFloatSetting("raftAirGapLayer0_mm")
                    + zReduction;

            boolean aModelIsOffTheBed = false;
            for (ProjectifiableThing projectifiableThing : currentProject.getTopLevelThings())
            {
                if (projectifiableThing instanceof ModelContainer)
                {
                    ModelContainer modelContainer = (ModelContainer) projectifiableThing;

                    //TODO use settings derived offset values for spiral
                    if (modelContainer.isOffBedProperty().get()
                            || (currentProject.getPrinterSettings().getRaftOverride()
                            && modelContainer.isModelTooHighWithOffset(raftOffset))
                            || (currentProject.getPrinterSettings().getSpiralPrintOverride()
                            && modelContainer.isModelTooHighWithOffset(0.5)))
                    {
                        aModelIsOffTheBed = true;
                        break;
                    }
                }
            }

            if (!aModelIsOffTheBed)
            {

                Runnable runUpdateFields = () ->
                {
                    for(PrintQualityEnumeration printQuality : sliceOrder) {
                        switch(printQuality) {
                            case DRAFT:
                                updateFieldsForQuality(project, PrintQualityEnumeration.DRAFT, lblDraftTime,
                                lblDraftWeight,
                                lblDraftCost, cancellable);
                                break;
                            case NORMAL:
                                updateFieldsForQuality(project, PrintQualityEnumeration.NORMAL, lblNormalTime,
                                lblNormalWeight,
                                lblNormalCost, cancellable);
                                break;
                            case FINE:
                                updateFieldsForQuality(project, PrintQualityEnumeration.FINE, lblFineTime,
                                lblFineWeight,
                                lblFineCost, cancellable);
                                break;
                            case CUSTOM:
                                if (!currentProject.getPrinterSettings().getSettingsName().equals(""))
                                {
                                    updateFieldsForQuality(project, PrintQualityEnumeration.CUSTOM, lblCustomTime,
                                            lblCustomWeight,
                                            lblCustomCost, cancellable);
                                }
                                break;
                        }
                        if (cancellable.cancelled().get())
                        {
                            return;
                        }
                    };
                };

                timeCostThreadManager.cancelRunningTimeCostTasksAndRun(runUpdateFields, cancellable);
            }
        }
    }

/**
     * Update the time, cost and weight fields for the given print quality and fields.
     * Long running calculations must be performed in a background thread.
     */
    private void updateFieldsForQuality(Project project, PrintQualityEnumeration printQuality,
            Label lblTime, Label lblWeight, Label lblCost, Cancellable cancellable)
    {
        if (project instanceof ModelContainerProject)
        {
            String working = Lookup.i18n("timeCost.working");
            BaseLookup.getTaskExecutor().runOnGUIThread(() ->
            {
                lblTime.setText(working);
                lblWeight.setText(working);
                lblCost.setText(working);
            });

            GetTimeWeightCost updateDetails = new GetTimeWeightCost((ModelContainerProject) project,
                        lblTime, lblWeight,
                        lblCost, cancellable);

            updateDetails.updateFromProject(printQuality);
        }
    }

    private void updatePrintQuality(PrinterSettingsOverrides printerSettings)
    {
        switch (printerSettings.getPrintQuality())
        {
            case DRAFT:
                rbDraft.setSelected(true);
                break;
            case NORMAL:
                rbNormal.setSelected(true);
                break;
            case FINE:
                rbFine.setSelected(true);
                break;
            case CUSTOM:
                rbCustom.setSelected(true);
                break;
        }
    }

    @Override
    public void shutdownController()
    {

        if (currentProject != null && currentProject instanceof ModelContainerProject)
            ((ModelContainerProject)currentProject).getGCodeGenManager().getDataChangedProperty().removeListener(this.gCodePrepChangeListener);
        currentProject = null;

        ApplicationStatus.getInstance()
                .modeProperty().removeListener(applicationModeChangeListener);
    }
    
    private SlicerType getSlicerType() {
        return Lookup.getUserPreferences().getSlicerType();
    }
}
