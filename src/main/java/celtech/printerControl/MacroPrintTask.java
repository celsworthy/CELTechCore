/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.printerControl.model.Printer;
import celtech.coreUI.DisplayManager;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.rx.GCodeDataResponse;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.utils.SystemUtils;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class MacroPrintTask extends Task<String>
{

    private final Stenographer steno = StenographerFactory.getStenographer(MacroPrintTask.class.getName());
    private ArrayList<String> macroData = null;
    private Printer printer = null;
    private RoboxCommsManager commsManager = null;
    private String portName = null;
    private ResourceBundle i18nBundle = null;

    /**
     *
     * @param macroData
     * @param printer
     * @param commsManager
     * @param portName
     */
    public MacroPrintTask(ArrayList<String> macroData, Printer printer, RoboxCommsManager commsManager, String portName)
    {
        this.macroData = macroData;
        this.printer = printer;
        this.commsManager = commsManager;
        this.portName = portName;

        updateTitle("Macro print task");

        i18nBundle = DisplayManager.getLanguageBundle();
    }

    @Override
    protected String call() throws Exception
    {
        StringBuilder finalResponse = new StringBuilder();
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.EXECUTE_GCODE);
        steno.info("Macro print task started");

        for (String gcodeToSend : macroData)
        {
            if (gcodeToSend.equalsIgnoreCase(" ") == false)
            {
                gcodeToSend = SystemUtils.cleanGCodeForTransmission(gcodeToSend);
                gcodeToSend += "\n";
                gcodePacket.setMessagePayload(gcodeToSend);

//            while (printer.getBusy() && !isCancelled())
//            {
//                Thread.sleep(250);
//            }
                if (isCancelled())
                {
                    break;
                }

                GCodeDataResponse response = (GCodeDataResponse) commsManager.submitForWrite(portName, gcodePacket);
                finalResponse.append(response.getGCodeResponse() + "\n");
            }
        }

        steno.info("Macro print task ended");
        return finalResponse.toString();
    }

}
