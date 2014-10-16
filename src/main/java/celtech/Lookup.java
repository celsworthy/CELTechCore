/*
 * Copyright 2014 CEL UK
 */
package celtech;

import celtech.appManager.SystemNotificationManager;
import celtech.appManager.SystemNotificationManagerJavaFX;
import celtech.configuration.ApplicationEnvironment;
import celtech.utils.PrinterListChangesNotifier;
import celtech.utils.tasks.LiveTaskExecutor;
import celtech.utils.tasks.TaskExecutor;
import java.util.Locale;
import java.util.ResourceBundle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class Lookup
{
    private static Lookup instance;
    private ApplicationEnvironment applicationEnvironment;
    private TaskExecutor taskExecutor;
    private SystemNotificationManager systemNotificationHandler;
    private final Stenographer steno = StenographerFactory.getStenographer(Lookup.class.getName());
    private PrinterListChangesNotifier printerListChangeNotifier;

    /**
     * @return the applicationEnvironment
     */
    public static ApplicationEnvironment getApplicationEnvironment()
    {
        return instance.applicationEnvironment;
    }
    
    public static String i18n(String stringId) {
        return instance.applicationEnvironment.getLanguageBundle().getString(stringId);
    }

    /**
     * @param applicationEnvironment the applicationEnvironment to set
     */
    public static void setApplicationEnvironment(ApplicationEnvironment applicationEnvironment)
    {
        instance.applicationEnvironment = applicationEnvironment;
    }

    private Lookup()
    {
        Locale appLocale = Locale.getDefault();
        ResourceBundle i18nBundle = ResourceBundle.getBundle("celtech.resources.i18n.LanguageData", appLocale, new UTF8Control());
        applicationEnvironment = new ApplicationEnvironment(i18nBundle, appLocale);
        taskExecutor = new LiveTaskExecutor();
        systemNotificationHandler = new SystemNotificationManagerJavaFX();
        steno.info("Detected locale - " + appLocale.toLanguageTag());
//        printerListChangeNotifier = new PrinterListChangesNotifier(null);
    }

    public static void initialise()
    {
        instance = new Lookup();
    }

    public static TaskExecutor getTaskExecutor()
    {
        return instance.taskExecutor;
    }

    public static void setTaskExecutor(TaskExecutor taskExecutor)
    {
        instance.taskExecutor = taskExecutor;
    }

    public static SystemNotificationManager getSystemNotificationHandler()
    {
        return instance.systemNotificationHandler;
    }

    public static void setSystemNotificationHandler(SystemNotificationManager systemNotificationHandler)
    {
        instance.systemNotificationHandler = systemNotificationHandler;
    }
}
