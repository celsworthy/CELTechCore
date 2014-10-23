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
public class Slic3rConfigWriter extends SlicerConfigWriter
{
    public Slic3rConfigWriter()
    {
        super();
        slicerType = SlicerType.Slic3r;
    }

    @Override
    protected void outputLine(File outputFile, String variableName, boolean value) throws IOException
    {
        int valueToWrite = (value)?1:0;
        FileUtils.writeStringToFile(outputFile, variableName + " = " + valueToWrite + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, int value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + " = " + value + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, float value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + " = " + threeDPformatter.format(value) + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, String value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + " = " + value + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, SlicerType value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + " = " + value + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, FillPattern value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + " = " + value.name().toLowerCase() + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, SupportPattern value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + " = " + value.name().toLowerCase() + "\n", true);
    }

    @Override
    protected void outputPrintCentre(File outputFile, float centreX, float centreY) throws IOException
    {
        outputLine(outputFile, "print_center", (int)centreX + "," + (int)centreY);
    }

    @Override
    protected void outputFilamentDiameter(File outputFile, float diameter) throws IOException
    {
        outputLine(outputFile, "filament_diameter", String.format(Locale.UK, "%f", diameter));
    }
}
