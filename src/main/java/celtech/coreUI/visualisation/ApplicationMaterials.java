/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation;

import celtech.utils.gcode.representation.MovementType;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ApplicationMaterials
{

    private static final PhongMaterial defaultModelMaterial = new PhongMaterial(Color.DARKORCHID);
    private static final PhongMaterial selectedModelMaterial = new PhongMaterial(Color.LAWNGREEN);
    private static final PhongMaterial collidedModelMaterial = new PhongMaterial(Color.DARKORANGE);
    private static final PhongMaterial collidedSelectedModelMaterial = new PhongMaterial(Color.PERU);
    private static final PhongMaterial offBedModelMaterial = new PhongMaterial(Color.CRIMSON);
    
    //GCode-related materials
    private static final PhongMaterial extrusionMaterial = new PhongMaterial(Color.GREEN);
    private static final PhongMaterial retractMaterial = new PhongMaterial(Color.RED);
    private static final PhongMaterial unretractMaterial = new PhongMaterial(Color.BLUE);
    private static final PhongMaterial supportMaterial = new PhongMaterial(Color.BROWN);
    private static final PhongMaterial travelMaterial = new PhongMaterial(Color.LIGHTGREEN);
    private static final PhongMaterial pickedGCodeMaterial = new PhongMaterial(Color.GOLDENROD);

    public static PhongMaterial getDefaultModelMaterial()
    {
        defaultModelMaterial.setSpecularColor(Color.WHITE);
        defaultModelMaterial.setSpecularPower(1.5);
        return defaultModelMaterial;
    }

    public static PhongMaterial getSelectedModelMaterial()
    {
        return selectedModelMaterial;
    }

    public static PhongMaterial getCollidedModelMaterial()
    {
        return collidedModelMaterial;
    }

    public static PhongMaterial getCollidedSelectedModelMaterial()
    {
        return collidedSelectedModelMaterial;
    }
    
    public static PhongMaterial getOffBedModelMaterial()
    {
        return offBedModelMaterial;
    }

    public static Material getExtrusionMaterial()
    {
        return extrusionMaterial;
    }

    public static Material getRetractMaterial()
    {
        return retractMaterial;
    }

    public static Material getUnretractMaterial()
    {
        return unretractMaterial;
    }

    public static Material getSupportMaterial()
    {
        return supportMaterial;
    }

    public static Material getTravelMaterial()
    {
        return travelMaterial;
    }

    public static Material getGCodeMaterial(MovementType movementType, boolean selected)
    {
        Material returnVal = extrusionMaterial;
        
        if (selected == false)
        {
            switch (movementType)
            {
                case EXTRUDE:
                    returnVal = extrusionMaterial;
                    break;
                case EXTRUDE_SUPPORT:
                    returnVal = supportMaterial;
                    break;
                case RETRACT:
                    returnVal = retractMaterial;
                    break;
                case TRAVEL:
                    returnVal = travelMaterial;
                    break;
                case UNRETRACT:
                    returnVal = unretractMaterial;
                    break;
                default:
                    break;
            }
        } else
        {
            switch (movementType)
            {
                default:
                    returnVal = pickedGCodeMaterial;
                    break;
            }
        }
        
        return returnVal;
    }
}
