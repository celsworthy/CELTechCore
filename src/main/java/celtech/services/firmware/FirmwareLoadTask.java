/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.firmware;

import celtech.coreUI.DisplayManager;
import celtech.printerControl.model.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.exceptions.SDCardErrorException;
import celtech.utils.SystemUtils;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ResourceBundle;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class FirmwareLoadTask extends Task<Integer>
{

    /**
     *
     */
    public static final int SDCARD_ERROR = -1;

    /**
     *
     */
    public static final int SUCCESS = 0;

    /**
     *
     */
    public static final int FILE_ERROR = -2;

    /**
     *
     */
    public static final int OTHER_ERROR = -3;

    private String firmwareFileToLoad = null;
    private final Stenographer steno = StenographerFactory.getStenographer(this.getClass().getName());
    private Printer printerToUpdate = null;

    /**
     *
     * @param firmwareFileToLoad
     * @param printerToUpdate
     */
    public FirmwareLoadTask(String firmwareFileToLoad, Printer printerToUpdate)
    {
        this.firmwareFileToLoad = firmwareFileToLoad;
        this.printerToUpdate = printerToUpdate;
    }

    @Override
    protected Integer call() throws Exception
    {
        ResourceBundle languageBundle = DisplayManager.getLanguageBundle();

        int returnValue = OTHER_ERROR;
        try
        {
            File file = new File(firmwareFileToLoad);
            byte[] fileData = new byte[(int) file.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();

            int remainingBytes = fileData.length;
            int bufferPosition = 0;
            String firmwareID = SystemUtils.generate16DigitID();
            boolean sendOK = printerToUpdate.initialiseDataFileSend(firmwareID);

            if (sendOK)
            {
                updateTitle(languageBundle.getString("dialogs.firmwareUpdateProgressTitle"));
                updateMessage(languageBundle.getString("dialogs.firmwareUpdateProgressLoading"));

                while (bufferPosition < fileData.length && !isCancelled())
                {
                    updateProgress(bufferPosition, fileData.length);
                    byte byteToOutput = fileData[bufferPosition];
                    String byteAsString = String.format("%02X", byteToOutput);

                    printerToUpdate.sendDataFileChunk(byteAsString, remainingBytes == 1, false);

                    remainingBytes--;
                    bufferPosition++;
                }

                if (!isCancelled())
                {
                    printerToUpdate.transmitUpdateFirmware(firmwareID);
                    // 
                    returnValue = SUCCESS;
                }
            }
        }catch (SDCardErrorException ex)
        {
            steno.error("SD card exception whilst updating firmware");
            returnValue = SDCARD_ERROR;
        }catch (RoboxCommsException ex)
        {
            steno.error("Other comms exception whilst updating firmware " + ex);
            returnValue = OTHER_ERROR;
        } catch (IOException ex)
        {
            steno.error("Couldn't load firmware file " + ex.toString());
            returnValue = FILE_ERROR;
        }

        return returnValue;
    }
}
