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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class RestrictedTextArea extends TextArea
{

    private StringProperty restrict = new SimpleStringProperty();
    private IntegerProperty maxLength = new SimpleIntegerProperty(-1);

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

    public IntegerProperty maxLengthProperty()
    {
        return maxLength;
    }

    /**
     * Sets a regular expression character class which restricts the user
     * input.<br/>
     * E.g. [0-9] only allows numeric values.
     *
     * @param restrict The regular expression.
     */
    public void setRestrict(String restrict)
    {
        this.restrict.set(restrict);
    }

    public String getRestrict()
    {
        return restrict.get();
    }

    public StringProperty restrictProperty()
    {
        return restrict;
    }

    public RestrictedTextArea()
    {

        textProperty().addListener(new ChangeListener<String>()
        {

            private boolean ignore;

            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s1)
            {
                if (ignore || s1 == null)
                {
                    return;
                }
                if (maxLength.get() > -1 && s1.length() > maxLength.get())
                {
                    ignore = true;
                    setText(s1.substring(0, maxLength.get()));
                    ignore = false;
                }

                if (getSelection().getLength() > 0)
                {
                    int startIndex = getSelection().getStart();
                    int endIndex = getSelection().getEnd();
                    StringBuilder updatedString = new StringBuilder(s1);
                    updatedString.replace(getSelection().getStart(), getSelection().getEnd(), "");
                    ignore = true;
                    setText(updatedString.toString());
                    ignore = false;
                }

                if (restrict.get() != null && !restrict.get().equals("") && !s1.matches(restrict.get() + "*"))
                {
                    ignore = true;
                    setText(s);
                    ignore = false;
                }
            }
        });
    }

}
