/*
 * Copyright 2014 CEL UK
 */

package celtech.services.printing;

import celtech.appManager.Notifier;

/**
 *
 * @author tony
 */
public class NotificationsHandlerImpl implements NotificationsHandler
{

    @Override
    public void showInformationNotification(String title, String message)
    {
        Notifier.showInformationNotification(title, message);
    }

    @Override
    public void showWarningNotification(String title, String message)
    {
        Notifier.showWarningNotification(title, message);
    }

    @Override
    public void showErrorNotification(String title, String message)
    {
        Notifier.showErrorNotification(title, message);
    }
    
}
