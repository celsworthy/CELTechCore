package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.SlicerType;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.SlicerConfigWriter;
import celtech.configuration.slicer.SlicerConfigWriterFactory;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.services.postProcessor.GCodePostProcessingResult;
import celtech.services.postProcessor.PostProcessorTask;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SliceResult;
import celtech.services.slicer.SlicerTask;
import celtech.utils.SystemUtils;
import celtech.utils.threed.ThreeDUtils;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class TimeCostInsetPanelController implements Initializable
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

    private final SlicerParametersFile draftSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.draftSettingsProfileName);
    private final SlicerParametersFile normalSettings = SlicerParametersContainer.
        getSettingsByProfileName(
            ApplicationConfiguration.normalSettingsProfileName);
    private final SlicerParametersFile fineSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.fineSettingsProfileName);

    private Project currentProject;
    private PrinterSettings printerSettings = null;

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            Lookup.getSelectedProjectProperty().addListener(
                (ObservableValue<? extends Project> observable, Project oldValue, Project newValue) ->
                {
                    whenProjectChanged(newValue);
                });

            ApplicationStatus.getInstance().modeProperty().addListener(
                (ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue) ->
                {
                    if (newValue == ApplicationMode.SETTINGS)
                    {
                        timeCostInsetRoot.setVisible(true);
                    } else
                    {
                        timeCostInsetRoot.setVisible(false);
                    }
                });
            
            if (Lookup.getSelectedProjectProperty().get() != null) {
                updateFields(Lookup.getSelectedProjectProperty().get());
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void whenProjectChanged(Project project)
    {
        currentProject = project;
        printerSettings = project.getPrinterSettings();
        if (project != null)
        {
            updateFields(project);
        }
    }

    /**
     * Update the time, cost and weight fields. Long running calculations must be performed in a
     * background thread.
     */
    private void updateFields(Project project)
    {
        updateFieldsForProfile(project, draftSettings, lblDraftTime, lblDraftWeight, lblDraftCost);
        updateFieldsForProfile(project, normalSettings, lblNormalTime, lblNormalWeight, lblNormalCost);
        updateFieldsForProfile(project, fineSettings, lblFineTime, lblFineWeight, lblFineCost);
    }

    /**
     * Update the time, cost and weight fields for the given profile and fields. Long running
     * calculations must be performed in a background thread.
     */
    private void updateFieldsForProfile(Project project, SlicerParametersFile settings,
        Label lblTime, Label lblWeight, Label lblCost)
    {
        lblTime.setText("...");
        lblWeight.setText("...");
        lblCost.setText("...");

        GetTimeWeightCostTask updateDetails = new GetTimeWeightCostTask(project, settings,
                                                                        lblTime, lblWeight,
                                                                        lblCost);

        updateDetails.setOnFailed((WorkerStateEvent event) ->
        {
            lblTime.setText("NA");
            lblWeight.setText(String.valueOf("NA"));
            lblCost.setText(String.valueOf("NA"));
        });

        Thread th = new Thread(updateDetails);
        th.setDaemon(true);
        th.start();

    }

    class GetTimeWeightCostTask extends Task<Void>
    {

        private final Project project;
        private final SlicerParametersFile settings;
        private final Label lblTime;
        private final Label lblWeight;
        private final Label lblCost;

        public GetTimeWeightCostTask(Project project, SlicerParametersFile settings,
            Label lblTime, Label lblWeight, Label lblCost)
        {
            this.project = project;
            this.settings = settings;
            this.lblTime = lblTime;
            this.lblWeight = lblWeight;
            this.lblCost = lblCost;
        }

        /**
         * Update the time/cost/weight fields based on the given statistics.
         */
        private void updateFieldsForStatistics(PrintJobStatistics printJobStatistics)
        {
            double duration = printJobStatistics.getLayerNumberToPredictedDuration().stream().mapToDouble(
                Double::doubleValue).sum();

            String formattedDuration = formatDuration(duration);
            
            double volumeUsed = printJobStatistics.getVolumeUsed();
            //TODO make work with dual extruders
            Filament filament = project.getPrinterSettings().getFilament0();
            double weight = filament.getWeightForVolume(volumeUsed * 1e-9);
            String formattedWeight = formatWeight(weight);
            
            double costGBP = filament.getCostForVolume(volumeUsed * 1e-9);
            String formattedCost = formatCost(costGBP);
            
            lblTime.setText(formattedDuration);
            lblWeight.setText(formattedWeight);
            lblCost.setText(formattedCost);
        }

        @Override
        protected Void call() throws Exception
        {
            try
            {
                SlicerTask slicerTask = makeSlicerTask(project, settings);
                slicerTask.setOnSucceeded((WorkerStateEvent event) ->
                {
                    try
                    {
                        SliceResult sliceResult = slicerTask.getValue();

                        PostProcessorTask postProcessorTask = new PostProcessorTask(
                            sliceResult.getPrintJobUUID(),
                            settings, null);
                        postProcessorTask.setOnSucceeded((WorkerStateEvent event1) ->
                        {
                            try
                            {
                                GCodePostProcessingResult result = postProcessorTask.getValue();
                                PrintJobStatistics printJobStatistics = result.getRoboxiserResult().getPrintJobStatistics();

                                updateFieldsForStatistics(printJobStatistics);

                            } catch (Exception ex)
                            {
                                ex.printStackTrace();
                            }
                        });
                        postProcessorTask.setOnFailed((WorkerStateEvent event1) ->
                        {
                            lblDraftTime.setText("NA");
                            lblDraftWeight.setText("NA");
                            lblDraftCost.setText("NA");
                        });
                        Lookup.getTaskExecutor().runTaskAsDaemon(postProcessorTask);
                    } catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }

                });
                Lookup.getTaskExecutor().runTaskAsDaemon(slicerTask);

            } catch (Exception ex)
            {
                ex.printStackTrace();
            }
            return null;
        }

        /**
         * Set up a print job directory etc and return a SlicerTask based on it.
         */
        private SlicerTask makeSlicerTask(Project project, SlicerParametersFile settings)
        {
            //Create the print job directory
            String printUUID = SystemUtils.generate16DigitID();
            String printJobDirectoryName = ApplicationConfiguration.
                getPrintSpoolDirectory() + printUUID;
            File printJobDirectory = new File(printJobDirectoryName);
            printJobDirectory.mkdirs();

            //Write out the slicer config
            SlicerType slicerTypeToUse = null;
            if (settings.getSlicerOverride() != null)
            {
                slicerTypeToUse = settings.getSlicerOverride();
            } else
            {
                slicerTypeToUse = Lookup.getUserPreferences().getSlicerType();
            }

            SlicerConfigWriter configWriter = SlicerConfigWriterFactory.getConfigWriter(
                slicerTypeToUse);

            //We need to tell the slicers where the centre of the printed objects is - otherwise everything is put in the centre of the bed...
            Vector3D centreOfPrintedObject = ThreeDUtils.calculateCentre(project.getLoadedModels());
            configWriter.setPrintCentre((float) (centreOfPrintedObject.getX()
                + ApplicationConfiguration.xPrintOffset),
                                        (float) (centreOfPrintedObject.getZ()
                                        + ApplicationConfiguration.yPrintOffset));
            configWriter.generateConfigForSlicer(settings,
                                                 printJobDirectoryName
                                                 + File.separator
                                                 + printUUID
                                                 + ApplicationConfiguration.printProfileFileExtension);

            return new SlicerTask(printUUID, project, PrintQualityEnumeration.DRAFT,
                                  settings, null);

        }

        /**
         * Take the duration in seconds and return a string in the format H MM.
         */
        private String formatDuration(double duration)
        {
            int SECONDS_PER_HOUR = 3600;
            int numHours = (int) (duration / SECONDS_PER_HOUR);
            int numMinutes = (int) ((duration - (numHours * SECONDS_PER_HOUR)) / 60);
            return String.format("%sh %sm", numHours, numMinutes);
        }
        
        /**
         * Take the weight in grammes and return a string in the format NNNg.
         */
        private String formatWeight(double weight)
        {
            return String.format("%sg", (int) weight);
        }   
        
        /**
         * Take the cost in pounds and return a string in the format $1.43.
         */
        private String formatCost(double cost)
        {
            int numPounds = (int) cost;
            int numPence = (int) (cost - (numPounds * 100));
            return String.format("£%s.%02d", numPounds, numPence);
        }        

    }

}
