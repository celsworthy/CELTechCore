/*
 * Copyright 2014 CEL UK
 */
package celtech;

import celtech.appManager.Project;
import celtech.appManager.SystemNotificationManager;
import celtech.appManager.SystemNotificationManagerJavaFX;
import celtech.roboxbase.ApplicationEnvironment;
import celtech.configuration.Languages;
import celtech.configuration.UserPreferences;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.datafileaccessors.SlicerMappingsContainer;
import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.configuration.fileRepresentation.SlicerMappings;
import celtech.coreUI.ProjectGUIState;
import celtech.coreUI.SpinnerControl;
import celtech.coreUI.components.ChoiceLinkDialogBox;
import celtech.coreUI.components.Notifications.NotificationDisplay;
import celtech.coreUI.controllers.panels.MenuInnerPanel;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.GCodeOutputWriterFactory;
import celtech.gcodetranslator.LiveGCodeOutputWriter;
import celtech.printerControl.model.Printer;
import celtech.utils.PrinterListChangesNotifier;
import celtech.utils.tasks.LiveTaskExecutor;
import celtech.utils.tasks.TaskExecutor;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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

    private static ApplicationEnvironment applicationEnvironment;
    private static TaskExecutor taskExecutor;
    private static SystemNotificationManager systemNotificationHandler;
    private static final Stenographer steno = StenographerFactory.getStenographer(
        Lookup.class.getName());
    private static PrinterListChangesNotifier printerListChangesNotifier;
    private static final ObservableList<Printer> connectedPrinters;
    private static boolean shuttingDown = false;
    private static NotificationDisplay notificationDisplay;

    static
    {
        connectedPrinters = FXCollections.observableArrayList();
        connectedPrinters.addListener((ListChangeListener.Change<? extends Printer> change) ->
        {
            while (change.next())
            {
                if (change.wasRemoved())
                {
                    ChoiceLinkDialogBox.whenPrinterDisconnected();
                }
            }
        });
    }

    /**
     * The UserPreferences being used by the application.
     */
    private static UserPreferences userPreferences;
    private static SlicerMappings slicerMappings;
    /**
     * The SpinnerControl being used by the GUI.
     */
    private static SpinnerControl spinnerControl;
    /**
     * The printer that has been selected on the Status panel.
     */
    private static final ObjectProperty<Printer> currentlySelectedPrinterProperty = new SimpleObjectProperty<>();
    /**
     * The selectedProject is the project that has most recently been selected on the ProjectTab
     * control.
     */
    private static final ObjectProperty<Project> selectedProject = new SimpleObjectProperty<>();
    /**
     * Each Project has a ProjectGUIState that holds all the necessary GUI state for the Project eg
     * selectionModel.
     */
    private static final Map<Project, ProjectGUIState> projectGUIStates = new HashMap<>();
    private static final Languages languages = new Languages();
    private static GCodeOutputWriterFactory<GCodeOutputWriter> postProcessorGCodeOutputWriterFactory;
    
    /**
     * The database of known filaments.
     */
    private static FilamentContainer filamentContainer;

    private static ResourceBundle defaultBundle;

    public static Languages getLanguages()
    {
        return languages;
    }

    /**
     * @return the applicationEnvironment
     */
    public static ApplicationEnvironment getApplicationEnvironment()
    {
        return applicationEnvironment;
    }

    public static FilamentContainer getFilamentContainer()
    {
        return filamentContainer;
    }

    public static String i18n(String stringId)
    {
        String langString = applicationEnvironment.getLanguageBundle().getString(stringId);
        if (langString.equals(""))
        {
            if (defaultBundle == null)
            {
                defaultBundle = ResourceBundle.getBundle("celtech.resources.i18n.LanguageData",
                                                         new UTF8Control());
            }
            langString = defaultBundle.getString(stringId);
        }
        langString = substituteTemplates(langString);
        return langString;
    }

    /**
     * Strings containing templates (eg *T14) should be substituted with the correct text.
     */
    static String substituteTemplates(String langString)
    {
        String patternString = ".*\\*T(\\d\\d).*";
        Pattern pattern = Pattern.compile(patternString);
        while (true)
        {
            Matcher matcher = pattern.matcher(langString);
            if (matcher.find())
            {
                String template = "*T" + matcher.group(1);
                String templatePattern = "\\*T" + matcher.group(1);
                langString = langString.replaceAll(templatePattern, i18n(template));
            } else
            {
                break;
            }
        }
        return langString;
    }

    public static ResourceBundle getLanguageBundle()
    {
        return applicationEnvironment.getLanguageBundle();
    }

    /**
     * @param applicationEnvironment the applicationEnvironment to set
     */
    public static void setApplicationEnvironment(ApplicationEnvironment applicationEnvironment)
    {
        Lookup.applicationEnvironment = applicationEnvironment;
    }

    public static void setupDefaultValues()
    {
        steno.debug("Starting AutoMaker - get user preferences...");
        userPreferences = new UserPreferences(UserPreferenceContainer.getUserPreferenceFile());

        StenographerFactory.changeAllLogLevels(userPreferences.getLoggingLevel());

        Locale appLocale;
        String languageTag = userPreferences.getLanguageTag();
        steno.debug("Starting AutoMaker - language tag is " + languageTag);
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
                    appLocale = new Locale(languageElements[0], languageElements[1],
                                           languageElements[2]);
                    break;
                default:
                    appLocale = Locale.getDefault();
                    break;
            }
        }

        steno.debug("Starting AutoMaker - loading resources...");
        ResourceBundle i18nBundle = ResourceBundle.getBundle("celtech.resources.i18n.LanguageData",
                                                             appLocale, new UTF8Control());
        applicationEnvironment = new ApplicationEnvironment(i18nBundle, appLocale);
        taskExecutor = new LiveTaskExecutor();
        systemNotificationHandler = new SystemNotificationManagerJavaFX();
        steno.debug("Detected locale - " + appLocale.toLanguageTag());
        printerListChangesNotifier = new PrinterListChangesNotifier(connectedPrinters);

        filamentContainer = new FilamentContainer();

        slicerMappings = SlicerMappingsContainer.getSlicerMappings();

        setPostProcessorOutputWriterFactory(LiveGCodeOutputWriter::new);
        
        setNotificationDisplay(new NotificationDisplay());
    }

    public static TaskExecutor getTaskExecutor()
    {
        return taskExecutor;
    }

    public static void setTaskExecutor(TaskExecutor taskExecutor)
    {
        Lookup.taskExecutor = taskExecutor;
    }

    public static SystemNotificationManager getSystemNotificationHandler()
    {
        return systemNotificationHandler;
    }

    public static void setSystemNotificationHandler(
        SystemNotificationManager systemNotificationHandler)
    {
        Lookup.systemNotificationHandler = systemNotificationHandler;
    }

    public static SpinnerControl getSpinnerControl()
    {
        return spinnerControl;
    }

    public static void setSpinnerControl(
        SpinnerControl spinnerControl)
    {
        Lookup.spinnerControl = spinnerControl;
    }

    public static PrinterListChangesNotifier getPrinterListChangesNotifier()
    {
        return printerListChangesNotifier;
    }

    public static ObservableList<Printer> getConnectedPrinters()
    {
        return connectedPrinters;
    }

    public static ReadOnlyObjectProperty<Printer> getSelectedPrinterProperty()
    {
        return currentlySelectedPrinterProperty;
    }

    public static void setSelectedPrinter(Printer currentlySelectedPrinter)
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

    public static void setPostProcessorOutputWriterFactory(
        GCodeOutputWriterFactory<GCodeOutputWriter> factory)
    {
        postProcessorGCodeOutputWriterFactory = factory;
    }

    public static ObjectProperty<Project> getSelectedProjectProperty()
    {
        return selectedProject;
    }

    public static void setSelectedProject(Project project)
    {
        selectedProject.set(project);
    }

    public static ProjectGUIState getProjectGUIState(Project project)
    {
        if (!projectGUIStates.containsKey(project))
        {
            ProjectGUIState projectGUIState = new ProjectGUIState(project);
            projectGUIStates.put(project, projectGUIState);
        }
        return projectGUIStates.get(project);
    }

    public static NotificationDisplay getNotificationDisplay()
    {
        return notificationDisplay;
    }

    public static void setNotificationDisplay(NotificationDisplay notificationDisplay)
    {
        Lookup.notificationDisplay = notificationDisplay;
    }

    public static boolean isShuttingDown()
    {
        return shuttingDown;
    }

    public static void setShuttingDown(boolean shuttingDown)
    {
        Lookup.shuttingDown = shuttingDown;
    }
}
