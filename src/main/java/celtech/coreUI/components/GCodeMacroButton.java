package celtech.coreUI.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;

/**
 *
 * @author Ian
 */
public class GCodeMacroButton extends Button
{
    private StringProperty macroName = new SimpleStringProperty("");
    
    public void setMacroName(String value)
    {
        macroName.set(value);
    }
    
    public String getMacroName()
    {
        return macroName.get();
    }
    
    public StringProperty macroNameProperty()
    {
        return macroName;
    }
}
