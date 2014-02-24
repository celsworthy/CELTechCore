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
    private StringProperty restrict = new SimpleStringProperty();
    private IntegerProperty maxLength = new SimpleIntegerProperty(-1);
    private BooleanProperty forceUpperCase = new SimpleBooleanProperty(false);

    private String standardAllowedCharacters = "\u0008";

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

    public void setForceUpperCase(boolean forceUpperCase)
    {
        this.forceUpperCase.set(forceUpperCase);
    }

    public boolean getForceUpperCase()
    {
        return this.forceUpperCase.get();
    }

    public BooleanProperty forceUpperCaseProperty()
    {
        return forceUpperCase;
    }

    /**
     * Sets a regular expression character class which restricts the user
     * input.<br/>
     * E.g. 0-9 only allows numeric values.
     *
     * @param restrict The regular expression.
     */
    public void setRestrict(String restrict)
    {
        this.restrict.set("[" + restrict + standardAllowedCharacters + "]+");
    }

    public String getRestrict()
    {
        return restrict.get();
    }

    public StringProperty restrictProperty()
    {
        return restrict;
    }

    public RestrictedTextField()
    {
        this.getStyleClass().add(this.getClass().getSimpleName());
//        this.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>()
//        {
//
//            @Override
//            public void handle(KeyEvent t)
//            {
//                if (getSelection().getLength() > 0 && t.getText().matches("[a-zA-Z0-9.\\-? ]+"))
//                {
//                    replaceSelection("");
//                    t.consume();
//                }
//            }
//        });
//
//        this.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>()
//        {
//            @Override
//            public void handle(KeyEvent t)
//            {
//                if (forceUpperCase.get())
//                {
//                    insertText(getCaretPosition(), t.getCharacter().toUpperCase());
//                    t.consume();
//                }
//            }
//        });
//
//        textProperty().addListener(new ChangeListener<String>()
//        {
//
//            private boolean ignore;
//
//            @Override
//            public void changed(ObservableValue<? extends String> observableValue, String s, String s1)
//            {
//                if (ignore || s1 == null)
//                {
//                    return;
//                }
//                if (maxLength.get() > -1 && s1.length() > maxLength.get())
//                {
//                    ignore = true;
//                    setText(s1.substring(0, maxLength.get()));
//                    ignore = false;
//                }
//
////                if (getSelection().getLength() > 0)
////                {
////                    int startIndex = getSelection().getStart();
////                    int endIndex = getSelection().getEnd();
////                    StringBuilder updatedString = new StringBuilder(s);
////                    updatedString = updatedString.replace(getSelection().getStart(), getSelection().getEnd(), "");
////                    ignore = true;
////                    setText(updatedString.toString());
////                    ignore = false;
////                }
//                if (restrict.get() != null && !restrict.get().equals("") && !s1.matches(restrict.get() + "*"))
//                {
//                    ignore = true;
//                    setText(s);
//                    ignore = false;
//                }
//            }
//        });

    }

    @Override
    public void replaceText(int start, int end, String text)
    {
        if (forceUpperCase.getValue())
        {
            text = text.toUpperCase();
        }
        int length = this.getText().length() + text.length() - (end - start);

        if ((text.matches(restrict.get()) && length <= maxLength.getValue()) || text.equals(""))
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
