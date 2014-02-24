/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SidePanel
{

    private HBox sidePanelNode;
    private Object sidePanelController;

    SidePanel(HBox sidePanelNode, Object sidePanelController)
    {
        this.sidePanelNode = sidePanelNode;
        this.sidePanelController = sidePanelController;
    }

    public HBox getSidePanelNode()
    {
        return sidePanelNode;
    }

    public void setSidePanelNode(HBox sidePanelNode)
    {
        this.sidePanelNode = sidePanelNode;
    }

    public Object getSidePanelController()
    {
        return sidePanelController;
    }

    public void setSidePanelController(Object sidePanelController)
    {
        this.sidePanelController = sidePanelController;
    }

    public VBox getSlideOutContainer()
    {
        VBox returnValue = null;

        for (Node node : sidePanelNode.getChildren())
        {
            if (node.getId().equalsIgnoreCase("SlideOut") && node instanceof HBox)
            {
                for (Node subNode : ((HBox)node).getChildren())
                {
                    if (subNode.getId().equalsIgnoreCase("Container") && subNode instanceof VBox)
                    {
                        returnValue = (VBox) subNode;
                        break;
                    }
                }
            }
        }

        return returnValue;
    }
}
