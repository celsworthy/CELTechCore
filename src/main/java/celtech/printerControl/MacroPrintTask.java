/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.coreUI.DisplayManager;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.rx.GCodeDataResponse;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 *
 * @author Ian
 */
public class MacroPrintTask extends Task<String>
{

    private ArrayList<String> macroData = null;
    private ObservableList<String> gcodeTranscript = null;
    private Printer printer = null;
    private RoboxCommsManager commsManager = null;
    private String portName = null;
    private boolean addToTranscript = false;
    private ResourceBundle i18nBundle = null;

    public MacroPrintTask(ArrayList<String> macroData, ObservableList<String> gcodeTranscript, Printer printer, RoboxCommsManager commsManager, String portName, boolean addToTranscript)
    {
        this.macroData = macroData;
        this.gcodeTranscript = gcodeTranscript;
        this.printer = printer;
        this.commsManager= commsManager;
        this.portName = portName;
        this.addToTranscript = addToTranscript;

        i18nBundle = DisplayManager.getLanguageBundle();
    }

    @Override
    protected String call() throws Exception
    {
        StringBuilder finalResponse = new StringBuilder();
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.EXECUTE_GCODE);

        for (String gcodeToSend : macroData)
        {
            gcodeToSend += "\n";
            gcodePacket.setMessagePayload(gcodeToSend);

            while (printer.busyProperty().get() == true && isCancelled() == false)
            {
                Thread.sleep(250);
            }
            
            if (isCancelled())
            {
                break;
            }
            
            GCodeDataResponse response = (GCodeDataResponse) commsManager.submitForWrite(portName, gcodePacket);
            finalResponse.append(response.getGCodeResponse() + "\n");

            final String gcodeSent = gcodeToSend;
            
            if (addToTranscript)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        gcodeTranscript.add(gcodeSent);
                        if (response == null)
                        {
                            gcodeTranscript.add(i18nBundle.getString("gcodeEntry.errorMessage"));
                        } else if (!response.getGCodeResponse().trim().equals(""))
                        {
                            gcodeTranscript.add(response.getGCodeResponse());
                        }
                    }
                });
            }
            
            Thread.sleep(100);
        }
        return finalResponse.toString();
    }

}
