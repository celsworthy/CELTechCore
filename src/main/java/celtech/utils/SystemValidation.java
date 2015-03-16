/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.MachineType;
import java.util.ResourceBundle;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.dialog.Dialogs;

/**
 * The SystemValidation class houses functions that validate the host system
 * such as 3D support.
 *
 * @author tony
 */
public class SystemValidation
{

    private static final Stenographer steno = StenographerFactory.getStenographer(SystemValidation.class.getName());

    /**
     * Check that the machine type is fully recognised and if not then exit the
     * application.
     */
    public static void checkMachineTypeRecognised(ResourceBundle i18nBundle)
    {
        MachineType machineType = ApplicationConfiguration.getMachineType();
        if (machineType.equals(MachineType.UNKNOWN))
        {
            Dialogs.create()
                    .owner(null)
                    .title(i18nBundle.getString("dialogs.fatalErrorDetectingMachineType"))
                    .masthead(null)
                    .message(i18nBundle.getString("dialogs.automakerUnknownMachineType"))
                    .showError();
            steno.error("Closing down due to unrecognised machine type.");
            Platform.exit();
        }
    }

    /**
     * Check that 3D is supported on this machine and if not then exit the
     * application.
     * @param i18nBundle
     * @return 
     */
    public static boolean check3DSupported(ResourceBundle i18nBundle)
    {
        boolean threeDSupportOK = false;
        
        steno.debug("Starting AutoMaker - check 3D support...");
        boolean checkForScene3D = true;

        String forceGPU = System.getProperty("prism.forceGPU");

        if (forceGPU != null)
        {
            if (forceGPU.equalsIgnoreCase("true"))
            {
                checkForScene3D = false;
            }
        }

        if (checkForScene3D == true)
        {
            if (!Platform.isSupported(ConditionalFeature.SCENE3D))
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Dialogs.create()
                                .owner(null)
                                .title(i18nBundle.getString("dialogs.fatalErrorNo3DSupport"))
                                .masthead(null)
                                .message(i18nBundle.getString("dialogs.automakerErrorNo3DSupport"))
                                .showError();
                        steno.error("Closing down due to lack of required 3D support.");
                        Platform.exit();
                    }
                });
            }
            else
            {
                threeDSupportOK = true;
            }
        }
        
        return threeDSupportOK;
    }

}
