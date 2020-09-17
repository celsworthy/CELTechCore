package celtech.coreUI.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;

/**
 *
 * @author George Salter
 */
public class RestrictedComboBox<T> extends ComboBox<T>
{
    private final StringProperty restrict = new SimpleStringProperty("");
    private final IntegerProperty maxLength = new SimpleIntegerProperty(-1);
    private final BooleanProperty directorySafeName = new SimpleBooleanProperty(false);

    private final String standardAllowedCharacters = "\u0008\u007f"; // Backspace and Delete

    public RestrictedComboBox()
    {
        super();
        
        getEditor().textProperty().addListener(new ChangeListener<String>() 
        {
            private boolean ignore;
            
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) 
            {
                if (ignore || newValue == null)
                {
                    return;
                }

                String restrictedString = applyRestriction(newValue);

                if (newValue.length() > maxLength.get()) 
                {
                    restrictedString = restrictedString.substring(0, maxLength.get());
                }
                
                if (!restrictedString.isEmpty() && !restrictedString.matches(restrict.get())) 
                {
                    restrictedString = oldValue;
                }
                
                ignore = true;
                getEditor().setText(restrictedString);
                ignore = false;
            }
        });
    }
    
    /**
     * Sets a regular expression character class which restricts the user input.
     * E.g. 0-9 only allows numeric values.
     *
     * @param restrict The regular expression.
     */
    public void setRestrict(String restrict)
    {
        String restrictString = "[" + restrict + standardAllowedCharacters + "]+";
        this.restrict.set(restrictString);
    }

    /**
     *
     * @return
     */
    public String getRestrict()
    {
        return restrict.get();
    }

    /**
     *
     * @return
     */
    public StringProperty restrictProperty()
    {
        return restrict;
    }
    
    /**
     *
     * @return
     */
    public int getMaxLength()
    {
        return maxLength.get();
    }

    /**
     * Sets the max length of the text field.
     *
     * @param maxLength The max length.
     */
    public void setMaxLength(int maxLength)
    {
        this.maxLength.set(maxLength);
    }

    /**
     *
     * @return
     */
    public IntegerProperty maxLengthProperty()
    {
        return maxLength;
    }
    
    /**
     * @return the directorySafeName
     */
    public boolean getDirectorySafeName()
    {
        return directorySafeName.get();
    }

    /**
     * @param directorySafeName the directorySafeName to set
     */
    public void setDirectorySafeName(boolean directorySafeName)
    {
        this.directorySafeName.set(directorySafeName);
    }

    private String applyRestriction(String text)
    {
        if (directorySafeName.get())
        {
            for (char disallowedChar : "/<>:\"\\|?*".toCharArray())
            {
                char[] toReplace = new char[1];
                toReplace[0] = disallowedChar;
                text = text.replace(new String(toReplace), "");
            }
        }
        return text;
    }
}
