/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.popups;

import celtech.printerControl.comms.commands.rx.AckResponse;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 *
 * @author Ian
 */
public class GenericErrorPopupController implements Initializable
{

    @FXML
    private VBox container;

    @FXML
    private TextArea errorDisplay;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
    }

    public void populateErrorList(AckResponse errors)
    {
        StringBuilder errorList = new StringBuilder();

        if (errors.isBadCommandError())
        {
            errorList.append("Bad Command\n");
        }
        if (errors.isBadFirmwareFileError())
        {
            errorList.append("Bad firmware file\n");
        }
        if (errors.isChunkSequenceError())
        {
            errorList.append("Chunk sequence\n");
        }
        if (errors.isFileTooLargeError())
        {
            errorList.append("File too large\n");
        }
        if (errors.isFlashChecksumError())
        {
            errorList.append("Flash checksum\n");
        }
        if (errors.isGCodeBufferOverrunError())
        {
            errorList.append("GCode buffer overrun\n");
        }
        if (errors.isGcodeLineTooLongError())
        {
            errorList.append("GCode line too long\n");
        }
        if (errors.isReelEEPROMError())
        {
            errorList.append("Reel EEPROM error\n");
        }
        if (errors.isHeadEepromError())
        {
            errorList.append("Head EEPROM error\n");
        }
        if (errors.isSdCardError())
        {
            errorList.append("SD card error\n");
        }
        if (errors.isUsbRXError())
        {
            errorList.append("USB receive error\n");
        }
        if (errors.isUsbTXError())
        {
            errorList.append("USB transmit error\n");
        }

        errorDisplay.setText(errorList.toString());
    }
}
