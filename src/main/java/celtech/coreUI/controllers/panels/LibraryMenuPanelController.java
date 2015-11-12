package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.SlicerParametersFile;

/**
 *
 * @author Ian
 */
public class LibraryMenuPanelController extends MenuPanelController
{

    public LibraryMenuPanelController()
    {
        paneli18Name = "libraryMenu.title";
    }

    @Override
    protected void setupInnerPanels()
    {
        loadInnerPanel(
                ApplicationConfiguration.fxmlPanelResourcePath + "filamentLibraryPanel.fxml",
                new FilamentLibraryPanelController());

        profileDetailsController = new ProfileLibraryPanelController();
        profileDetails = loadInnerPanel(
                ApplicationConfiguration.fxmlUtilityPanelResourcePath + "profileDetails.fxml",
                profileDetailsController);
        
        loadInnerPanel(
                ApplicationConfiguration.fxmlPanelResourcePath + "MaintenanceInsetPanel.fxml",
                new MaintenanceInsetPanelController());
    }
    
        public void showAndSelectPrintProfile(SlicerParametersFile printProfile)
    {
        String profileMenuItemName = Lookup.i18n(profileDetails.innerPanel.getMenuTitle());
        panelMenu.selectItemOfName(profileMenuItemName);
        profileDetailsController.setAndSelectPrintProfile(printProfile);
    }
}
