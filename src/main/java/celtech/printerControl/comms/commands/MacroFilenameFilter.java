package celtech.printerControl.comms.commands;

import celtech.printerControl.model.Head;
import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author Ian
 */
public class MacroFilenameFilter implements FilenameFilter
{

    private final String baseMacroName;
    private final Head.HeadType headType;
    private final GCodeMacros.NozzleUseIndicator nozzleUse;
    private final GCodeMacros.SafetyIndicator safeties;
    private final String separator = "#";

    public MacroFilenameFilter(String baseMacroName,
            Head.HeadType headType,
            GCodeMacros.NozzleUseIndicator nozzleUse,
            GCodeMacros.SafetyIndicator safeties)
    {
        this.baseMacroName = baseMacroName;
        this.headType = headType;
        this.nozzleUse = nozzleUse;
        this.safeties = safeties;
    }

    @Override
    public boolean accept(File dir, String name)
    {
        boolean okToAccept = false;

        String stringToMatchAgainst = baseMacroName;

        if (safeties == GCodeMacros.SafetyIndicator.SAFETIES_OFF)
        {
            stringToMatchAgainst += separator + GCodeMacros.SafetyIndicator.SAFETIES_OFF.getFilenameCode();
        }

        if (nozzleUse != GCodeMacros.NozzleUseIndicator.DONT_CARE)
        {
            stringToMatchAgainst += separator + nozzleUse.getFilenameCode();
        }

        if (name.equalsIgnoreCase(stringToMatchAgainst))
        {
            okToAccept = true;
        }
        return okToAccept;
    }

}
