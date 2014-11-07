/*
 * Copyright 2014 CEL UK
 */
package celtech;

import celtech.appManager.SystemNotificationManager;
import celtech.appManager.SystemNotificationManagerJavaFX;
import celtech.configuration.ApplicationEnvironment;
import celtech.configuration.UserPreferences;
import celtech.configuration.datafileaccessors.SlicerMappingsContainer;
import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.configuration.fileRepresentation.SlicerMappings;
import celtech.configuration.fileRepresentation.SlicerParameters;
import celtech.printerControl.model.Printer;
import celtech.utils.PrinterListChangesNotifier;
import celtech.utils.tasks.LiveTaskExecutor;
import celtech.utils.tasks.TaskExecutor;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * This class functions as the global service registry for AutoMaker.
 * @author tony
 */
public class Lookup
{

    private static Lookup instance;
    private ApplicationEnvironment applicationEnvironment;
    private TaskExecutor taskExecutor;
    private SystemNotificationManager systemNotificationHandler;
    private final Stenographer steno = StenographerFactory.getStenographer(Lookup.class.getName());
    private static PrinterListChangesNotifier printerListChangesNotifier;
    private static ObservableList<Printer> connectedPrinters = FXCollections.observableArrayList();
    private static UserPreferences userPreferences;
    private static SlicerMappings slicerMappings;
    private static SlicerParameters slicerParameters;
    private static final ObjectProperty<Printer> currentlySelectedPrinterProperty = new SimpleObjectProperty<>();

    /**
     * @return the applicationEnvironment
     */
    public static ApplicationEnvironment getApplicationEnvironment()
    {
        return instance.applicationEnvironment;
    }

    public static String i18n(String stringId)
    {
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
//        Locale.setDefault(new Locale("zh", "CN")); 
        Locale appLocale = Locale.getDefault();
        ResourceBundle i18nBundle = ResourceBundle.getBundle("celtech.resources.i18n.LanguageData", appLocale, new UTF8Control());
        applicationEnvironment = new ApplicationEnvironment(i18nBundle, appLocale);
        taskExecutor = new LiveTaskExecutor();
        systemNotificationHandler = new SystemNotificationManagerJavaFX();
        steno.info("Detected locale - " + appLocale.toLanguageTag());
        printerListChangesNotifier = new PrinterListChangesNotifier(connectedPrinters);
        userPreferences = new UserPreferences(UserPreferenceContainer.getUserPreferenceFile());
        slicerMappings = SlicerMappingsContainer.getSlicerMappings();
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

    public static PrinterListChangesNotifier getPrinterListChangesNotifier()
    {
        return printerListChangesNotifier;
    }

    public static ObservableList<Printer> getConnectedPrinters()
    {
        return connectedPrinters;
    }

    /**
     *
     * @return
     */
    public static ReadOnlyObjectProperty<Printer> getCurrentlySelectedPrinterProperty()
    {
        return currentlySelectedPrinterProperty;
    }

    /**
     *
     * @param currentlySelectedPrinter
     */
    public static void setCurrentlySelectedPrinter(Printer currentlySelectedPrinter)
    {
        currentlySelectedPrinterProperty.set(currentlySelectedPrinter);
    }

    /**
     *
     * @return
     */
    public static ObjectProperty<Printer> currentlySelectedPrinterProperty()
    {
        return currentlySelectedPrinterProperty;
    }

    public static UserPreferences getUserPreferences()
    {
        return userPreferences;
    }

    public static SlicerMappings getSlicerMappings()
    {
        return slicerMappings;
    }
}
