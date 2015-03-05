/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels;

import java.net.URL;
import java.util.List;

/**
 * ExtrasMenuInnerPanel defines the properties needed to instantiate an inner panel from
 * the ExtrasMenuPanel.
 * 
 * @author tony
 */
public interface ExtrasMenuInnerPanel
{
    /**
     * An OperationButton defines what buttons are required to be shown when this
     * panel is displayed, and the callback to call when the button is pressed.
     */
    public interface OperationButton {
        
        /**
         * The i18n id of the text of the button.
         */
        public String getTextId();
        
        /**
         * Return the file location for the fxml that defines the button graphic.
         */
        public String getFXMLLocation();
        
        /**
         * The i18n id of the tooltip text.
         */
        public String getTooltipTextId();
    }
    
    /**
     * Return i18n id of the title to appear in the ExtrasMenu vertical menu.
     */
    public String getMenuTitle();
    
    /**
     * Return the URL for the fxml that defines the inner panel.
     */
    public URL getFXMLURL();
    
    /**
     * Return the list of OperationButtons that should be offered when this panel is displayed.
     */
    public List<OperationButton> getOperationButtons();
    
}
