/*
 * Copyright 2014 CEL UK
 */
package celtech.services.printing;

/**
 *
 * @author tony
 */
public interface NotificationsHandler
{

    public void showInformationNotification(String title, String message);

    /**
     *
     * @param title
     * @param message
     */
    public void showWarningNotification(String title, String message);

    /**
     *
     * @param title
     * @param message
     */
    public void showErrorNotification(String title, String message);
}
