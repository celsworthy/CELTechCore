package celtech.coreUI.components;

import javafx.scene.control.Tooltip;

/**
 *
 * @author Ian
 */
public class HideableTooltip extends Tooltip
{

    public HideableTooltip()
    {
        this.getStyleClass().add("hideableTooltip");
        this.setWrapText(true);
        this.setMaxWidth(600);
    }
    
}
