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
    STATUS("printerStatus"),

    /**
     *
     */
    LAYOUT("layout"),

    /**
     *
     */
    SETTINGS("settings");
    
    private final String contentFXMLPrefix;

    private ApplicationMode(String contentFXMLPrefix)
    {
        this.contentFXMLPrefix = contentFXMLPrefix;
    }

    /**
     *
     * @return
     */
    public String getSidePanelFXMLName()
    {
        return ApplicationConfiguration.fxmlSidePanelResourcePath + contentFXMLPrefix + "SidePanel" + ".fxml";
    }

    /**
     *
     * @return
     */
    public String getSlideOutFXMLName()
    {
        return ApplicationConfiguration.fxmlSidePanelResourcePath + contentFXMLPrefix + "SlideOutPanel" + ".fxml";
    }
}
