/*
 * Copyright 2014 CEL UK
 */
package celtech;

import celtech.appManager.Project;
import celtech.appManager.SystemNotificationManagerJavaFX;
import celtech.configuration.UserPreferences;
import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.coreUI.ProjectGUIState;
import celtech.coreUI.SpinnerControl;
import celtech.coreUI.components.ChoiceLinkDialogBox;
import celtech.coreUI.components.Notifications.NotificationDisplay;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.printerControl.model.Printer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * This class functions as the global service registry for AutoMaker.
 *
 * @author tony
 */
public class Lookup
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
            Lookup.class.getName());
    private static NotificationDisplay notificationDisplay;

    static
    {
        BaseLookup.getConnectedPrinters().addListener((ListChangeListener.Change<? extends Printer> change) ->
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
    /**
     * The SpinnerControl being used by the GUI.
     */
    private static SpinnerControl spinnerControl;
    /**
     * The printer that has been selected on the Status panel.
     */
    private static final ObjectProperty<Printer> currentlySelectedPrinterProperty = new SimpleObjectProperty<>();
    /**
     * The selectedProject is the project that has most recently been selected
     * on the ProjectTab control.
     */
    private static final ObjectProperty<Project> selectedProject = new SimpleObjectProperty<>();
    /**
     * Each Project has a ProjectGUIState that holds all the necessary GUI state
     * for the Project eg selectionModel.
     */
    private static final Map<Project, ProjectGUIState> projectGUIStates = new HashMap<>();

    public static void setupDefaultValues()
    {
        steno.debug("Starting AutoMaker - get user preferences...");
        userPreferences = new UserPreferences(UserPreferenceContainer.getUserPreferenceFile());

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

        BaseLookup.setupDefaultValues(userPreferences.getLoggingLevel(),
                appLocale, new SystemNotificationManagerJavaFX());

        setNotificationDisplay(new NotificationDisplay());
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

    public static String i18n(String stringId)
    {
        return BaseLookup.i18n(stringId);
    }
}
