/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.purge;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.HeaterMode;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.services.ControllableService;
import celtech.services.calibration.*;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import celtech.utils.PrinterUtils;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PurgeTask extends Task<Void> implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(PurgeTask.class.getName());
    private Project project = null;
    private Filament filament = null;
    private PrintQualityEnumeration printQuality = null;
    private RoboxProfile settings = null;
    private Printer printerToUse = null;

    public PurgeTask(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    public PurgeTask(Project project, Filament filament, PrintQualityEnumeration printQuality, RoboxProfile settings, Printer printerToUse)
    {
        this.project = project;
        this.filament = filament;
        this.printQuality = printQuality;
        this.settings = settings;
        this.printerToUse = printerToUse;
    }

    @Override
    protected Void call() throws Exception
    {
        // put the write after the purge routine once the firmware no longer raises an error whilst connected to the host computer
        HeadEEPROMDataResponse savedHeadData = printerToUse.transmitReadHeadEEPROM();
        AckResponse ackResponse = printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                                                       savedHeadData.getUniqueID(),
                                                                       savedHeadData.getMaximumTemperature(),
                                                                       savedHeadData.getBeta(),
                                                                       savedHeadData.getTCal(),
                                                                       savedHeadData.getNozzle1XOffset(),
                                                                       savedHeadData.getNozzle1YOffset(),
                                                                       savedHeadData.getNozzle1ZOffset(),
                                                                       savedHeadData.getNozzle1BOffset(),
                                                                       savedHeadData.getNozzle2XOffset(),
                                                                       savedHeadData.getNozzle2YOffset(),
                                                                       savedHeadData.getNozzle2ZOffset(),
                                                                       savedHeadData.getNozzle2BOffset(),
                                                                       (float) (printerToUse.getReelNozzleTemperature().get()),
                                                                       savedHeadData.getHeadHours());
        if (ackResponse.isNozzleFlushNeededError())
        {
            printerToUse.transmitResetErrors();
        }

        printerToUse.transmitStoredGCode("Purge Material");
        PrinterUtils.waitOnMacroFinished(printerToUse, this);

        if (project != null)
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    printerToUse.printProject(project, filament, printQuality, settings);
                }
            });
        }

        return null;
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

}
