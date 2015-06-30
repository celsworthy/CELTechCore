package celtech.configuration;

import celtech.Lookup;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public enum Macro
{
    //NOTE - don't change or reuse any of these macro job numbers - just keep on incrementing

    CANCEL_PRINT("abort_print", "printerStatus.macro.homing", "M1"),
    HOME_ALL("Home_all", "printerStatus.macro.homing", "M2"),
    EJECT_STUCK_MATERIAL("eject_stuck_material", "printerStatus.macro.ejectStuckMaterial", "M3"),
    LEVEL_GANTRY("level_gantry", "printerStatus.macro.levellingGantry", "M4"),
    LEVEL_GANTRY_TWO_POINTS("level_gantry", "printerStatus.macro.levellingGantry", "M5"),
    PURGE("Purge Material", "printerStatus.purging", "M6"),
    TEST_X("x_test", "printerStatus.macro.testX", "M7"),
    TEST_Y("y_test", "printerStatus.macro.testY", "M8"),
    TEST_Z("z_test", "printerStatus.macro.testZ", "M9"),
    LEVEL_Y("level_Y", "printerStatus.macro.levellingY", "M10");

    private String macroFileName;
    private String i18nKey;
    private String macroJobNumber;

    private Macro(String macroFileName,
            String i18nKey,
            String macroJobNumber)
    {
        this.macroFileName = macroFileName;
        this.i18nKey = i18nKey;
        this.macroJobNumber = macroJobNumber;
    }

    public String getMacroFileName()
    {
        return macroFileName;
    }

    public String getFriendlyName()
    {
        return Lookup.i18n(i18nKey);
    }

    public String getMacroJobNumber()
    {
        String jobNumber = String.format("%1$-16s", macroJobNumber).replace(' ', '-');
        return jobNumber;
    }
    
    public static Optional<Macro> getMacroForPrintJobID(String printJobID)
    {
        Optional<Macro> foundMacro = Optional.empty();
        
        for (Macro macro : Macro.values())
        {
            if (macro.getMacroJobNumber().equalsIgnoreCase(printJobID.trim()))
            {
                foundMacro = Optional.of(macro);
                break;
            }
        }
        
        return foundMacro;
    }
}
