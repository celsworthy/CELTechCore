/*
 * Copyright 2014 CEL UK
 */
package celtech;

import celtech.appManager.SystemNotificationManager;
import celtech.appManager.SystemNotificationManagerJavaFX;
import celtech.configuration.ApplicationEnvironment;
import celtech.configuration.Languages;
import celtech.configuration.UserPreferences;
import celtech.configuration.datafileaccessors.SlicerMappingsContainer;
import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.configuration.fileRepresentation.SlicerMappings;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.GCodeOutputWriterFactory;
import celtech.gcodetranslator.LiveGCodeOutputWriter;
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
    private static PrinterListChangesNotifier printerListChangesNotifier;
    private static final ObservableList<Printer> connectedPrinters = FXCollections.observableArrayList();
    private static UserPreferences userPreferences;
    private static SlicerMappings slicerMappings;
    private static SlicerParametersFile slicerParameters;
    private static final ObjectProperty<Printer> currentlySelectedPrinterProperty = new SimpleObjectProperty<>();
    private static Languages languages = new Languages();
    private static GCodeOutputWriterFactory<GCodeOutputWriter> postProcessorGCodeOutputWriterFactory;

    public static Languages getLanguages()
    {
        return languages;
    }

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
        steno.info("Starting AutoMaker - get user preferences...");
        userPreferences = new UserPreferences(UserPreferenceContainer.getUserPreferenceFile());

        Locale appLocale;
        String languageTag = userPreferences.getLanguageTag();
        steno.info("Starting AutoMaker - language tag is " + languageTag);
        if (languageTag == null || languageTag.length() == 0)
        {
            appLocale = Locale.getDefault();
        } else
        {
            String[] languageElements = languageTag.split("-");
            switch (languageElements.length)
            {
                case 1:
                    appLocale = new Locale(languageElements[0]);
                    break;
                case 2:
                    appLocale = new Locale(languageElements[0], languageElements[1]);
                    break;
                case 3:
                    appLocale = new Locale(languageElements[0], languageElements[1], languageElements[2]);
                    break;
                default:
                    appLocale = Locale.getDefault();
                    break;
            }
        }
        
        steno.info("Starting AutoMaker - loading resources...");
        ResourceBundle i18nBundle = ResourceBundle.getBundle("celtech.resources.i18n.LanguageData",
                                                             appLocale, new UTF8Control());
        applicationEnvironment = new ApplicationEnvironment(i18nBundle, appLocale);
        taskExecutor = new LiveTaskExecutor();
        systemNotificationHandler = new SystemNotificationManagerJavaFX();
        steno.info("Detected locale - " + appLocale.toLanguageTag());
        printerListChangesNotifier = new PrinterListChangesNotifier(connectedPrinters);

        slicerMappings = SlicerMappingsContainer.getSlicerMappings();

        setPostProcessorOutputWriterFactory(LiveGCodeOutputWriter::new);
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

    public static ReadOnlyObjectProperty<Printer> getCurrentlySelectedPrinterProperty()
    {
        return currentlySelectedPrinterProperty;
    }

    public static void setCurrentlySelectedPrinter(Printer currentlySelectedPrinter)
    {
        currentlySelectedPrinterProperty.set(currentlySelectedPrinter);
    }

    public static UserPreferences getUserPreferences()
    {
        return userPreferences;
    }

    public static SlicerMappings getSlicerMappings()
    {
        return slicerMappings;
    }

    public static GCodeOutputWriterFactory getPostProcessorOutputWriterFactory()
    {
        return postProcessorGCodeOutputWriterFactory;
    }

    public static void setPostProcessorOutputWriterFactory(GCodeOutputWriterFactory<GCodeOutputWriter> factory)
    {
        postProcessorGCodeOutputWriterFactory = factory;
    }
}
