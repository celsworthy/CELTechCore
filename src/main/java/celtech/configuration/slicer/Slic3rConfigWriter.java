package celtech.configuration.slicer;

import celtech.configuration.SlicerType;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

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
    protected void outputLine(FileWriter writer, String variableName, boolean value) throws IOException
    {
        int valueToWrite = (value)?1:0;
        writer.append(variableName + " = " + valueToWrite + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, int value) throws IOException
    {
        writer.append(variableName + " = " + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, float value) throws IOException
    {
        writer.append(variableName + " = " + threeDPformatter.format(value) + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, String value) throws IOException
    {
        writer.append(variableName + " = " + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, SlicerType value) throws IOException
    {
        writer.append(variableName + " = " + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, FillPattern value) throws IOException
    {
        writer.append(variableName + " = " + value.name().toLowerCase() + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, SupportPattern value) throws IOException
    {
        writer.append(variableName + " = " + value.name().toLowerCase() + "\n");
    }

    @Override
    protected void outputPrintCentre(FileWriter writer, float centreX, float centreY) throws IOException
    {
        outputLine(writer, "print_center", (int)centreX + "," + (int)centreY);
    }

    @Override
    protected void outputFilamentDiameter(FileWriter writer, float diameter) throws IOException
    {
        outputLine(writer, "filament_diameter", String.format(Locale.UK, "%f", diameter));
    }
}
