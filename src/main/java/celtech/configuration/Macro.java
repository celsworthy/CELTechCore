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
//NOTE - don't change or reuse any of these macro job numbers - just keep on incrementing

    CANCEL_PRINT("abort_print", "printerStatus.macro.cancelling", "M1"),
    HOME_ALL("Home_all", "printerStatus.macro.homing", "M2"),
    LEVEL_GANTRY("level_gantry", "printerStatus.macro.levellingGantry", "M4"),
    LEVEL_GANTRY_TWO_POINTS("level_gantry", "printerStatus.macro.levellingGantry", "M5"),
    SPEED_TEST("speed_test", "printerStatus.macro.speedTest", "M6"),
    TEST_X("x_test", "printerStatus.macro.testX", "M7"),
    TEST_Y("y_test", "printerStatus.macro.testY", "M8"),
    TEST_Z("z_test", "printerStatus.macro.testZ", "M9"),
    LEVEL_Y("level_Y", "printerStatus.macro.levellingY", "M10"),
    CLEAN_NOZZLE_0("T0_nozzle_clean", "printerStatus.macro.cleanNozzle0", "M11"),
    CLEAN_NOZZLE_1("T1_nozzle_clean", "printerStatus.macro.cleanNozzle1", "M12"),
    
    PURGE_MATERIAL_SINGLE("PurgeMaterialSingle", "printerStatus.purging", "M13"),
    PURGE_MATERIAL_DUAL_0("PurgeMaterialDual0", "printerStatus.purging", "M14"),
    PURGE_MATERIAL_DUAL_1("PurgeMaterialDual1", "printerStatus.purging", "M15"),
    PURGE_MATERIAL_DUAL_BOTH("PurgeMaterialDualBoth", "printerStatus.purging", "M16"),
    
    EJECT_STUCK_MATERIAL_E("eject_stuck_material_e", "printerStatus.macro.ejectStuckMaterial", "M18"),
    EJECT_STUCK_MATERIAL_D("eject_stuck_material_d", "printerStatus.macro.ejectStuckMaterial", "M19"),
    
    REMOVE_HEAD("Remove_Head", "printerStatus.macro.removeHead", "M20"),
    // Commissionator macros
    COMMISSIONING_XMOTOR("x_commissioning", "printerStatus.macro.testX", "C1"),
    COMMISSIONING_YMOTOR("y_commissioning", "printerStatus.macro.testY", "C2"),
    COMMISSIONING_ZMOTOR_DIRECTION("commissioning_level_gantry_test", "printerStatus.macro.testZ", "C3");

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
