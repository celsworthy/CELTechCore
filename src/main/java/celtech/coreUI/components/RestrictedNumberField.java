package celtech.coreUI.components;

import celtech.Lookup;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class RestrictedNumberField extends TextField
{

    private final IntegerProperty maxLength = new SimpleIntegerProperty(0);
    private final IntegerProperty allowedDecimalPlaces = new SimpleIntegerProperty(0);
    private final IntegerProperty intValue = new SimpleIntegerProperty(-1);
    private final FloatProperty floatValue = new SimpleFloatProperty(-1);
    private final DoubleProperty doubleValue = new SimpleDoubleProperty(-1);
    private final BooleanProperty allowNegative = new SimpleBooleanProperty(false);
    private Pattern restrictionPattern = Pattern.compile("[0-9]+");

    private NumberFormat numberFormatter = null;

    private final String standardAllowedCharacters = "[\u0008\u007f0-9]+";
    private String restriction = "[0-9]+";
    private String decimalSeparator = null;

    private ChangeListener<Number> valueChangeListener = null;

    private boolean suppressTextToNumberUpdate = false;

    
    public boolean getAllowNegative() {
        return allowNegative.get();
    }
    
    public void setAllowNegative(boolean allowNegative) {
        this.allowNegative.set(allowNegative);
    }    
    
    public BooleanProperty allowNegativeProperty() {
        return allowNegative;
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
        configureRestriction();
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

        NumberFormat numberFormatter = getNumberFormatter();

        if (numberFormatter != null)
        {
            numberFormatter.setMaximumFractionDigits(allowedDecimalPlaces.get());
            numberFormatter.setMinimumFractionDigits(allowedDecimalPlaces.get());
        }
        configureRestriction();
    }

    private void configureRestriction()
    {
        String newRestriction = null;
        if (allowedDecimalPlaces.get() > 0 && maxLength.get() > allowedDecimalPlaces.get())
        {
            newRestriction = "[0-9]{0," + (maxLength.get() - allowedDecimalPlaces.get() - 1) + "}(?:\\" + getDecimalSeparator() + "[0-9]{0," + allowedDecimalPlaces.get() + "})?";
        } else if (allowedDecimalPlaces.get() > 0)
        {
            newRestriction = "[0-9]+(?:\\" + getDecimalSeparator() + "[0-9]{0," + allowedDecimalPlaces.get() + "})?";
        } else
        {
            newRestriction = "[0-9]{0," + maxLength.get() + "}";
        }
        if (allowNegative.get()) {
            newRestriction = "-?" + newRestriction;
        }
        restrictionPattern = Pattern.compile(newRestriction);
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
        
        setText("-1");

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

    private void enforceRestriction(String oldText, IndexRange oldSelectionRange)
    {
        Matcher m = restrictionPattern.matcher(this.getText());
        if (!m.matches())
        {
            this.setText(oldText);
            this.selectRange(oldSelectionRange.getStart(), oldSelectionRange.getEnd());
        }
    }

    @Override
    public void replaceText(int start, int end, String text)
    {
        String oldText = this.getText();
        IndexRange oldSelectionRange = this.getSelection();

        super.replaceText(start, end, text);

        enforceRestriction(oldText, oldSelectionRange);
        updateNumberValues();
    }

    private void updateNumberValues()
    {
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

    @Override
    public void replaceSelection(String text)
    {
        String oldText = this.getText();
        IndexRange oldSelectionRange = this.getSelection();

        super.replaceSelection(text);

        enforceRestriction(oldText, oldSelectionRange);
        updateNumberValues();
    }

    @Override
    public void replaceText(IndexRange range, String text)
    {
        String oldText = this.getText();
        IndexRange oldSelectionRange = this.getSelection();

        super.replaceText(range, text);

        enforceRestriction(oldText, oldSelectionRange);
        updateNumberValues();
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
            try
            {
                Locale usersLocale = Lookup.getApplicationEnvironment().getAppLocale();
                numberFormatter = NumberFormat.getInstance(usersLocale);
            } catch (NoClassDefFoundError ex)
            {
                //We should only be here if we're being loaded by Scene Builder
                numberFormatter = NumberFormat.getInstance();
            }
            numberFormatter.setMaximumFractionDigits(allowedDecimalPlaces.get());
            numberFormatter.setMinimumFractionDigits(allowedDecimalPlaces.get());
        }

        return numberFormatter;
    }

    private String getDecimalSeparator()
    {
        if (decimalSeparator == null)
        {
            try
            {
                decimalSeparator = Character.toString(new DecimalFormatSymbols(Lookup.getApplicationEnvironment().getAppLocale()).getDecimalSeparator());
            } catch (NoClassDefFoundError ex)
            {
                //We should only be here if we're being loaded by Scene Builder
                decimalSeparator = ".";
            }
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
