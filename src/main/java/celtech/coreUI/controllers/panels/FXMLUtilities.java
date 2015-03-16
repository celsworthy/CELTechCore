/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 *
 * @author tony
 */
public class FXMLUtilities
{

    /**
     * Add colons to all descendent labels that have the styleclass "colon".
     *
     * @param parentNode the node from which to start the recursion.
     */
    public static void addColonsToLabels(Parent parentNode)
    {
        if (parentNode instanceof Label)
        {
            Label label = (Label) parentNode;
            if (label.getStyleClass().contains("colon"))
            {
                label.setText(label.getText() + ":");
            }
        } else
        {
            for (Node node : parentNode.getChildrenUnmodifiable())
            {
                if (node instanceof Parent)
                {
                    addColonsToLabels((Parent) node);
                }
                if (node instanceof TabPane)
                {
                    TabPane tabPane = (TabPane) node;
                    for (Tab tab : tabPane.getTabs())
                    {
                        Node content = tab.getContent();
                        if (content instanceof Parent)
                        {
                            addColonsToLabels((Parent) content);
                        }
                    }

                }
            }
        }
    }

}
