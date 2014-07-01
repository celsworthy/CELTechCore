/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextField;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class RestrictedTextField extends TextField
{

    private final StringProperty restrict = new SimpleStringProperty("");
    private final IntegerProperty maxLength = new SimpleIntegerProperty(-1);
    private final BooleanProperty forceUpperCase = new SimpleBooleanProperty(false);

    private final String standardAllowedCharacters = "\u0008\u007f";

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
     *
     * @param forceUpperCase
     */
    public void setForceUpperCase(boolean forceUpperCase)
    {
        this.forceUpperCase.set(forceUpperCase);
    }

    /**
     *
     * @return
     */
    public boolean getForceUpperCase()
    {
        return this.forceUpperCase.get();
    }

    /**
     *
     * @return
     */
    public BooleanProperty forceUpperCaseProperty()
    {
        return forceUpperCase;
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
     */
    public RestrictedTextField()
    {
        this.getStyleClass().add(this.getClass().getSimpleName());
    }

    @Override
    public void replaceText(int start, int end, String text)
    {
        if (forceUpperCase.getValue())
        {
            text = text.toUpperCase();
        }
        int length = this.getText().length() + text.length() - (end - start);

        if ( //Control characters - always let them through
                text.equals("")
                || (text.matches(restrict.get()) && length <= maxLength.getValue()))
        {
            super.replaceText(start, end, text);
        }
    }

    @Override
    public void replaceSelection(String text)
    {
        if (forceUpperCase.getValue())
        {
            text = text.toUpperCase();
        }
        int length = this.getText().length() + text.length();

        if ((text.matches(restrict.get()) && length <= maxLength.getValue()) || text.equals(""))
        {
            super.replaceSelection(text);
        }
    }
}
