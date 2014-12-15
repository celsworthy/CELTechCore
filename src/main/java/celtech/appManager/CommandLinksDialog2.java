/*
 * Copyright 2014 CEL UK
 */
package celtech.appManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import org.controlsfx.DialogResources;
import org.controlsfx.dialog.CommandLinksDialog;

/**
 *
 * @author tony
 */
class CommandLinksDialog2 extends Dialog<ButtonType> {
    
    public static class CommandLinksButtonType {
        private final ButtonType buttonType;
        private final String longText;
        private final Node graphic;
        
        public CommandLinksButtonType(String text, boolean isDefault ) {
            this(new ButtonType(text, buildButtonData(isDefault)), null);
        }
        
        public CommandLinksButtonType(String text, String longText, boolean isDefault) {
            this(new ButtonType(text, buildButtonData(isDefault)), longText, null);
        }
        
        public CommandLinksButtonType(String text, String longText, Node graphic, boolean isDefault) {
            this(new ButtonType(text, buildButtonData(isDefault)), longText, graphic);
        }
        
        private CommandLinksButtonType(ButtonType buttonType) {
            this(buttonType, null);
        }
        
        private CommandLinksButtonType(ButtonType buttonType, String longText) {
            this(buttonType, longText, null);
        }
        
        private CommandLinksButtonType(ButtonType buttonType, String longText, Node graphic) {
            this.buttonType = buttonType;
            this.longText = longText;
            this.graphic = graphic;
            
        }
        
        private static ButtonBar.ButtonData buildButtonData( boolean isDeafault) {
        	return isDeafault? ButtonBar.ButtonData.OK_DONE :ButtonBar.ButtonData.CANCEL_CLOSE;
        }
        
        public ButtonType getButtonType() {
            return buttonType;
        }
        
        public Node getGraphic() {
            return graphic;
        }
        
        public String getLongText() {
            return longText;
        }
    }
    
    
    private final static int gapSize = 10;
    
    private final Map<ButtonType, CommandLinksButtonType> typeMap;
    
    private Label contentTextLabel;
    
    private GridPane grid = new GridPane() {
        @Override protected double computePrefWidth(double height) {
            boolean isDefault = true;
            double pw = 0;

            for (ButtonType buttonType : getDialogPane().getButtonTypes()) {
                Button button = (Button) getDialogPane().lookupButton(buttonType);
                double buttonPrefWidth = button.getGraphic().prefWidth(-1);
                
                if (isDefault) {
                    pw = buttonPrefWidth;
                    isDefault = false;
                } else {
                    pw = Math.min(pw, buttonPrefWidth);
                }
            }
            return pw + gapSize;
        }

        @Override protected double computePrefHeight(double width) {
            double ph = getDialogPane().getHeader() == null ? 0 : 10;

            for (ButtonType buttonType : getDialogPane().getButtonTypes()) {
                Button button = (Button) getDialogPane().lookupButton(buttonType);
                ph += button.prefHeight(width) + gapSize;
            }
            
            // TODO remove magic number
            return ph * 1.5;
        }
    };
    
    public CommandLinksDialog2(CommandLinksButtonType... links) {
        this(Arrays.asList(links));
    }
    
    public CommandLinksDialog2(List<CommandLinksButtonType> links) {
        this.grid.setHgap(gapSize);
        this.grid.setVgap(gapSize);
        this.grid.getStyleClass().add("container"); //$NON-NLS-1$
        
        final DialogPane dialogPane = new DialogPane() {
            @Override protected Node createButtonBar() {
                return null;
            }
            
            @Override protected Node createButton(ButtonType buttonType) {
                return createCommandLinksButton(buttonType);
            }
        }; 
        setDialogPane(dialogPane);
        
        setTitle(DialogResources.getString("Dialog.info.title")); //$NON-NLS-1$
        dialogPane.getStyleClass().add("command-links-dialog"); //$NON-NLS-1$
        dialogPane.getStylesheets().add(CommandLinksDialog.class.getResource("dialogs.css").toExternalForm()); //$NON-NLS-1$
        dialogPane.getStylesheets().add(CommandLinksDialog.class.getResource("commandlink.css").toExternalForm()); //$NON-NLS-1$
        
        // create a map from ButtonType -> CommandLinkButtonType, and put the 
        // ButtonType values into the dialog pane
        typeMap = new HashMap<>();
        for (CommandLinksButtonType link : links) { 
            typeMap.put(link.getButtonType(), link); 
            dialogPane.getButtonTypes().add(link.getButtonType()); 
        }
        
        updateGrid();
        dialogPane.getButtonTypes().addListener((ListChangeListener<? super ButtonType>)c -> updateGrid());
        
        contentTextProperty().addListener(o -> updateContentText());
    }
    
    private void updateContentText() {
        String contentText = getDialogPane().getContentText();
        grid.getChildren().remove(contentTextLabel);
        if (contentText != null && ! contentText.isEmpty()) {
            if (contentTextLabel != null) {
                contentTextLabel.setText(contentText);
            } else {
                contentTextLabel = new Label(getDialogPane().getContentText());
                contentTextLabel.getStyleClass().add("command-link-message"); //$NON-NLS-1$
            }
            grid.add(contentTextLabel, 0, 0);
        }
    }
    
    private void updateGrid() {
        grid.getChildren().clear();
        
        // add the message to the top of the dialog
        updateContentText();
        
        // then build all the buttons
        int row = 1;
        for (final ButtonType buttonType : getDialogPane().getButtonTypes()) {
            if (buttonType == null) continue; 

            final Button button = (Button)getDialogPane().lookupButton(buttonType);   

            GridPane.setHgrow(button, Priority.ALWAYS);
            GridPane.setVgrow(button, Priority.ALWAYS);
            grid.add(button, 0, row++);
        }

//        // last button gets some extra padding (hacky)
//        GridPane.setMargin(buttons.get(buttons.size() - 1), new Insets(0,0,10,0));

        getDialogPane().setContent(grid);
        getDialogPane().requestLayout();
    }
    
    private Button createCommandLinksButton(ButtonType buttonType) {
        // look up the CommandLinkButtonType for the given ButtonType
        CommandLinksButtonType commandLink = typeMap.getOrDefault(buttonType, new CommandLinksButtonType(buttonType));
        
        // put the content inside a button
        final Button button = new Button();
        button.getStyleClass().addAll("command-link-button"); //$NON-NLS-1$
        button.setMaxHeight(Double.MAX_VALUE);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        
        final ButtonBar.ButtonData buttonData = buttonType.getButtonData();
        button.setDefaultButton(buttonData != null && buttonData.isDefaultButton());
        button.setOnAction(ae -> setResult(buttonType));

        final Label titleLabel = new Label(commandLink.getButtonType().getText() );
        titleLabel.minWidthProperty().bind(new DoubleBinding() {
            {
                bind(titleLabel.prefWidthProperty());
            }

            @Override protected double computeValue() {
                return titleLabel.getPrefWidth() + 400;
            }
        });
        titleLabel.getStyleClass().addAll("line-1"); //$NON-NLS-1$
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.TOP_LEFT);
        GridPane.setVgrow(titleLabel, Priority.NEVER);

        Label messageLabel = new Label(commandLink.getLongText() );
        messageLabel.getStyleClass().addAll("line-2"); //$NON-NLS-1$
        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.TOP_LEFT);
        messageLabel.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(messageLabel, Priority.SOMETIMES);

        Node commandLinkImage = commandLink.getGraphic();
        Node view = commandLinkImage == null ? 
                new ImageView(CommandLinksDialog.class.getResource("arrow-green-right.png").toExternalForm()) :  //$NON-NLS-1$
                commandLinkImage;
        Pane graphicContainer = new Pane(view);
        graphicContainer.getStyleClass().add("graphic-container"); //$NON-NLS-1$
        GridPane.setValignment(graphicContainer, VPos.TOP);
        GridPane.setMargin(graphicContainer, new Insets(0,10,0,0));

        GridPane grid = new GridPane();
        grid.minWidthProperty().bind(titleLabel.prefWidthProperty());
        grid.setMaxHeight(Double.MAX_VALUE);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.getStyleClass().add("container"); //$NON-NLS-1$
        grid.add(graphicContainer, 0, 0, 1, 2);
        grid.add(titleLabel, 1, 0);
        grid.add(messageLabel, 1, 1);

        button.setGraphic(grid);
        button.minWidthProperty().bind(titleLabel.prefWidthProperty());

        return button;
    }    
}
