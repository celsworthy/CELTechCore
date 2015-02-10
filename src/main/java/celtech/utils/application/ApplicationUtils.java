package celtech.utils.application;

import celtech.configuration.ApplicationConfiguration;
import java.util.Date;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class ApplicationUtils
{

    private static Stenographer steno = StenographerFactory.getStenographer(ApplicationUtils.class.
        getName());

    public static void outputApplicationBanner(Class parentClass)
    {
        steno.info("**********************************************************************");
        steno.info("Starting " + ApplicationConfiguration.getApplicationName());
        steno.info("Date " + new Date());
        steno.info("Version: " + ApplicationConfiguration.getApplicationVersion());
        steno.info("Installation directory: " + ApplicationConfiguration.
            getApplicationInstallDirectory(parentClass));
        steno.info("Machine type: " + ApplicationConfiguration.getMachineType());
        steno.info("Locale: " + ApplicationConfiguration.getUserPreferredLocale());
        steno.info("**********************************************************************");
    }
}
