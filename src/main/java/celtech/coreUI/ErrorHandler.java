/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.controllers.popups.GenericErrorPopupController;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class ErrorHandler
{

    private final Stenographer steno = StenographerFactory.getStenographer(ErrorHandler.class.getName());
    private ModalDialog errorDialog = null;
    private VBox genericErrorPopup = null;
    private GenericErrorPopupController genericErrorPopupController = null;
    private int errorsAcknowledged = 0;

    public ErrorHandler()
    {
        try
        {
            FXMLLoader genericErrorPageLoader = new FXMLLoader(getClass().getResource(ApplicationConfiguration.fxmlUtilityPanelResourcePath + "genericErrorPopup.fxml"), DisplayManager.getLanguageBundle());
            genericErrorPopup = genericErrorPageLoader.load();
            genericErrorPopupController = genericErrorPageLoader.getController();

            errorDialog = new ModalDialog(DisplayManager.getLanguageBundle().getString("genericErrorPopup.Errors"));
            errorDialog.setContent(genericErrorPopup);
            errorsAcknowledged = errorDialog.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Ok"));
        } catch (Exception ex)
        {
            steno.error("Failed to load error handler page");
        }
    }

    public void checkForErrors(Printer printer)
    {
        //Check for errors and open a Dialog if there are any present
        try
        {
            AckResponse errors = printer.transmitReportErrors();
            if (errors.isError())
            {
                if (errorDialog.isShowing() == false)
                {
                    genericErrorPopupController.populateErrorList(errors);
                    errorDialog.show();
                    steno.info("Resetting errors");
                    printer.transmitResetErrors();
                }
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error whilst requesting error status.");
        }
    }
}
