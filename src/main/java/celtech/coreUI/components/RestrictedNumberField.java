/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.coreUI.DisplayManager;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class RestrictedNumberField extends TextField
{

    private final IntegerProperty maxLength = new SimpleIntegerProperty(-1);
    private final IntegerProperty allowedDecimalPlaces = new SimpleIntegerProperty(0);
    private final IntegerProperty intValue = new SimpleIntegerProperty(-1);
    private final FloatProperty floatValue = new SimpleFloatProperty(-1);
    private final DoubleProperty doubleValue = new SimpleDoubleProperty(-1);

    private NumberFormat numberFormatter = null;

    private final String standardAllowedCharacters = "[\u0008\u007f0-9]+";
    private String decimalSeparator = null;

    private ChangeListener<Number> valueChangeListener = null;

    private boolean suppressTextToNumberUpdate = false;

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
     * @return
     */
    public int getAllowedDecimalPlaces()
    {
        return allowedDecimalPlaces.get();
    }

    /**
     * Sets the number of decimal places allowed in this field.
     *
     * @param numberOfDecimalPlaces
     */
    public void setAllowedDecimalPlaces(int numberOfDecimalPlaces)
    {
        this.allowedDecimalPlaces.set(numberOfDecimalPlaces);
        getNumberFormatter().setMaximumFractionDigits(allowedDecimalPlaces.get());
        getNumberFormatter().setMinimumFractionDigits(allowedDecimalPlaces.get());
    }

    /**
     *
     * @return
     */
    public IntegerProperty allowedDecimalPlacesProperty()
    {
        return allowedDecimalPlaces;
    }

    /**
     *
     */
    public RestrictedNumberField()
    {
        this.getStyleClass().add(this.getClass().getSimpleName());

        valueChangeListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            if (!suppressTextToNumberUpdate)
            {
                setText(getNumberFormatter().format(newValue));
            }
        };

        intValue.addListener(valueChangeListener);
        floatValue.addListener(valueChangeListener);
        doubleValue.addListener(valueChangeListener);
    }

    @Override
    public void replaceText(int start, int end, String text)
    {
        boolean goForReplace = false;

        int length = this.getText().length() + text.length() - (end - start);
        String currentText = this.getText();

        boolean containsDP = currentText.contains(getDecimalSeparator());
        String afterRep = currentText.replaceFirst("[0-9]+\\" + getDecimalSeparator(), "");
        int numberOfCharsAfterDP = afterRep.length();
        int dpPosition = currentText.indexOf(getDecimalSeparator());

        if (text.equals(getDecimalSeparator()))
        {
            //Decimal place - only if we haven't already had one
            if (allowedDecimalPlaces.get() > 0 && !containsDP)
            {
                goForReplace = true;
            }
        } else if (text.equals(""))
        {
            //Control characters - always let them through
            goForReplace = true;
        } else if (//Number after the DP and still under the allowed chars
                (containsDP && start > dpPosition && numberOfCharsAfterDP < allowedDecimalPlaces.get() && text.matches(standardAllowedCharacters))
                || (start <= dpPosition && text.matches(standardAllowedCharacters) && length <= maxLength.getValue())
                || (!containsDP && text.matches(standardAllowedCharacters) && length <= maxLength.getValue()))
        {
            goForReplace = true;
        }

        if (goForReplace)
        {
            super.replaceText(start, end, text);
            try
            {
                suppressTextToNumberUpdate = true;
                intValue.set(getNumberFormatter().parse(this.getText()).intValue());
                floatValue.set(getNumberFormatter().parse(this.getText()).floatValue());
                doubleValue.set(getNumberFormatter().parse(this.getText()).doubleValue());
                suppressTextToNumberUpdate = false;
            } catch (ParseException ex)
            {
            }
        }
    }

    @Override
    public void replaceSelection(String text)
    {
        int length = this.getText().length() + text.length();

        if ((text.matches(standardAllowedCharacters) && length <= maxLength.getValue()) || text.equals(""))
        {
            super.replaceSelection(text);
        }
    }

    public int getAsInt() throws ParseException
    {
        return intValue.get();
    }

    public float getAsFloat() throws ParseException
    {
        return floatValue.get();
    }

    public double getAsDouble() throws ParseException
    {
        return doubleValue.get();
    }

    private NumberFormat getNumberFormatter()
    {
        if (numberFormatter == null)
        {
            numberFormatter = NumberFormat.getInstance(DisplayManager.getInstance().getUsersLocale());
            numberFormatter.setMaximumFractionDigits(allowedDecimalPlaces.get());
            numberFormatter.setMinimumFractionDigits(allowedDecimalPlaces.get());
        }

        return numberFormatter;
    }

    private String getDecimalSeparator()
    {
        if (decimalSeparator == null)
        {
            decimalSeparator = Character.toString(new DecimalFormatSymbols(DisplayManager.getInstance().getUsersLocale()).getDecimalSeparator());
        }

        return decimalSeparator;
    }

    public IntegerProperty intValueProperty()
    {
        return intValue;
    }

    public FloatProperty floatValueProperty()
    {
        return floatValue;
    }

    public DoubleProperty doubleValueProperty()
    {
        return doubleValue;
    }
}
