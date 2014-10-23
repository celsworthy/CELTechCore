package celtech.configuration.slicer;

import celtech.configuration.SlicerType;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Ian
 */
public class CuraConfigWriter extends SlicerConfigWriter
{

    public CuraConfigWriter()
    {
        super();
        slicerType = SlicerType.Cura;
    }

    @Override
    protected void outputLine(File outputFile, String variableName, boolean value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + "=" + value + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, int value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + "=" + value + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, float value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + "=" + threeDPformatter.format(value) + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, String value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + "=" + value + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, SlicerType value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + "=" + value + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, FillPattern value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + "=" + value + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, SupportPattern value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + "=" + value + "\n", true);
    }

    @Override
    protected void outputPrintCentre(File outputFile, float centreX, float centreY) throws IOException
    {
        outputLine(outputFile, "posx", (int) (centreX * 1000));
        outputLine(outputFile, "posy", (int) (centreY * 1000));
    }

    @Override
    protected void outputFilamentDiameter(File outputFile, float diameter) throws IOException
    {
        outputLine(outputFile, "filamentDiameter", String.format(Locale.UK, "%d", (int)(diameter * 1000)));
    }
}
