package celtech.coreUI.components;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import java.io.IOException;
import java.util.Optional;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Ian
 */
public class ChoiceLinkDialogBox extends VBox
{
    
    @FXML
    private Label title;
    
    @FXML
    private Label message;
    
    @FXML
    private VBox buttonContainer;
    
    private Stage dialogStage = null;
    
    private Optional<ChoiceLinkButton> chosenButton = Optional.empty();
    
    public ChoiceLinkDialogBox()
    {
        dialogStage = new Stage(StageStyle.UNDECORATED);
        
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
            "/celtech/resources/fxml/components/ChoiceLinkDialogBox.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        
        fxmlLoader.setClassLoader(this.getClass().getClassLoader());
        
        try
        {
            fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }
        
        Scene dialogScene = new Scene(this, Color.TRANSPARENT);
        dialogScene.getStylesheets().add(ApplicationConfiguration.getMainCSSFile());
        dialogStage.setScene(dialogScene);
        dialogStage.initOwner(DisplayManager.getMainStage());
        dialogStage.initModality(Modality.WINDOW_MODAL);
        
        getStyleClass().add("error-dialog");
    }
    
    public void setTitle(String i18nTitle)
    {
        title.setText(i18nTitle);
    }
    
    public void setMessage(String i18nMessage)
    {
        message.setText(i18nMessage);
    }
    
    public ChoiceLinkButton addChoiceLink(String i18Title, String i18nMessage)
    {
        ChoiceLinkButton button = new ChoiceLinkButton();
        button.setTitle(i18Title);
        button.setMessage(i18nMessage);
        configureButtonListener(button);
        
        return button;
    }

    public ChoiceLinkButton addChoiceLink(String i18Title)
    {
        ChoiceLinkButton button = new ChoiceLinkButton();
        button.setTitle(i18Title);
        configureButtonListener(button);
        
        return button;
    }
    
    private void configureButtonListener(ChoiceLinkButton button)
    {
        button.pressedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                chosenButton = Optional.of(button);
                dialogStage.close();
            }
        });
        buttonContainer.getChildren().add(button);
    }
    
    public ChoiceLinkButton addChoiceLink(ChoiceLinkButton preconfiguredButton)
    {
        configureButtonListener(preconfiguredButton);
        
        return preconfiguredButton;
    }

    /**
     *
     * @return
     */
    public Optional<ChoiceLinkButton> getUserInput()
    {
        dialogStage.showAndWait();
        
        return chosenButton;
    }

    /**
     *
     * @return
     */
    public boolean isShowing()
    {
        return dialogStage.isShowing();
    }
}
