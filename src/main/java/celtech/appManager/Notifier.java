/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.appManager;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.Notifications;

/**
 *
 * @author Ian
 */
public class Notifier
{

    private static final Stenographer steno = StenographerFactory.getStenographer(Notifier.class.getName());

    private static Notifications createNotification(String title, String message)
    {
        return Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(ApplicationConfiguration.notificationDisplayDelay)
                .position(ApplicationConfiguration.notificationPosition)
                .owner(DisplayManager.getMainStage());
    }

    public static void showInformationNotification(String title, String message)
    {
        steno.info("Info Notification: " + title + " : " + message);
        createNotification(title, message).showInformation();
    }

    public static void showWarningNotification(String title, String message)
    {
        steno.info("Warning Notification: " + title + " : " + message);
        createNotification(title, message).showWarning();
    }

    public static void showErrorNotification(String title, String message)
    {
        steno.info("Error Notification: " + title + " : " + message);
        createNotification(title, message).showError();
    }
}
