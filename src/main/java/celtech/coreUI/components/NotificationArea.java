package celtech.coreUI.components;

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
        this.progressDisplay = new ProgressDisplay();
        this.getChildren().add(progressDisplay);
    }

}
