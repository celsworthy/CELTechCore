package celtech.printerControl;

import celtech.Lookup;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author Ian
 */
public enum MacroType
{

    ABORT("macros.abort", false),
    GCODE_PRINT("macros.gcodePrint", true),
    STANDARD_MACRO("macros.standard", true);

    private final String i18nString;
    private final boolean interruptible;

    private MacroType(String i18nString, boolean interruptible)
    {
        this.i18nString = i18nString;
        this.interruptible = interruptible;
    }

    /**
     *
     * @return
     */
    public boolean isInterruptible()
    {
        return interruptible;
    }

    /**
     *
     * @return
     */
    public String getI18nString()
    {
        return Lookup.i18n(i18nString);
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return getI18nString();
    }
}
