/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.appManager;

import celtech.configuration.ApplicationConfiguration;

/**
 *
 * @author ianhudson
 */
public enum ApplicationMode
{

    /**
     *
     */
    STATUS("printerStatus", null),
    
    NOZZLE_OPEN_CALIBRATION("printerStatus", "CalibrationNozzleB"),
    NOZZLE_OFFSET_CALIBRATION("printerStatus", "CalibrationNozzleOffset"),
    PURGE("printerStatus", "purge"),

    ABOUT("printerStatus", "about"),
    PREFERENCES_TOP_LEVEL("printerStatus", "preferencesTop"),
    /**
     *
     */
    LAYOUT("layout", null),

    /**
     *
     */
    SETTINGS("settings", null);
    
    private final String sidePanelFXMLPrefix;
    private final String insetPanelFXMLPrefix;

    private ApplicationMode(String sidePanelFXMLPrefix, String insetPanelFXMLPrefix)
    {
        this.sidePanelFXMLPrefix = sidePanelFXMLPrefix;
        this.insetPanelFXMLPrefix = insetPanelFXMLPrefix;
    }

    /**
     *
     * @return
     */
    public String getSidePanelFXMLName()
    {
        return ApplicationConfiguration.fxmlPanelResourcePath + sidePanelFXMLPrefix + "SidePanel" + ".fxml";
    }

    /**
     *
     * @return
     */
    public String getSlideOutFXMLName()
    {
        return ApplicationConfiguration.fxmlPanelResourcePath + sidePanelFXMLPrefix + "SlideOutPanel" + ".fxml";
    }
    
        /**
     *
     * @return
     */
    public String getInsetPanelFXMLName()
    {
        return ApplicationConfiguration.fxmlPanelResourcePath + insetPanelFXMLPrefix + "InsetPanel" + ".fxml";
    }

}
