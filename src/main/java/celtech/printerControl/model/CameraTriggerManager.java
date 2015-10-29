package celtech.printerControl.model;

import celtech.utils.Math.MathUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CameraTriggerManager
{

    private Stenographer steno = StenographerFactory.getStenographer(CameraTriggerManager.class.getName());
    private Printer associatedPrinter = null;
    
    public CameraTriggerManager(Printer printer)
    {
        associatedPrinter = printer;
    }
    
    private ChangeListener<Boolean> cameraTriggerListener = new ChangeListener<Boolean>()
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            if (!oldValue && newValue)
            {
                triggerCamera();
            }
        }
    };
    
    public void listenForCameraTrigger()
    {
        steno.info("Started listening");
        associatedPrinter.getPrinterAncillarySystems().YStopSwitch.addListener(cameraTriggerListener);
    }
    
    public void stopListeningForCameraTrigger()
    {
        steno.info("Stopped listening");
        associatedPrinter.getPrinterAncillarySystems().YStopSwitch.removeListener(cameraTriggerListener);
    }
    
    private void triggerCamera()
    {
        
    }
}
