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

    WELCOME("Welcome"),
    CALIBRATION_CHOICE("Calibration"),
    REGISTRATION("registration"),
    PURGE("purge"),
    MAINTENANCE("Maintenance"),
    ABOUT("about"),
    SYSTEM_INFORMATION("systemInformation"),
    EXTRAS_MENU("extrasMenu"),
    //TODO printer status has to be last otherwise the temperature graph doesn't work!! Fix in DisplayManager
    STATUS(null),
    /**
     *
     */
    LAYOUT(null),
    ADD_MODEL("loadModel"),
    MY_MINI_FACTORY("myMiniFactoryLoader"),
    /**
     *
     */
    SETTINGS(null);

    private final String insetPanelFXMLPrefix;

    private ApplicationMode(String insetPanelFXMLPrefix)
    {
        this.insetPanelFXMLPrefix = insetPanelFXMLPrefix;
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
