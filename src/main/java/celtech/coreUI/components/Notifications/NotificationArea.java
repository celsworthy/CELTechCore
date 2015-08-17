package celtech.coreUI.components.Notifications;

import celtech.Lookup;
import javafx.scene.layout.VBox;

/**
 *
 * @author Ian
 */
public class NotificationArea extends VBox
{

    private final ProgressDisplay progressDisplay;

    public NotificationArea()
    {
        this.getChildren().add(Lookup.getNotificationDisplay());
        this.progressDisplay = new ProgressDisplay();
        this.getChildren().add(progressDisplay);
    }
}
