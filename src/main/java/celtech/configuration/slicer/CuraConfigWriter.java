package celtech.configuration.slicer;

import celtech.configuration.SlicerType;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

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
    protected void outputLine(FileWriter writer, String variableName, boolean value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, int value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, float value) throws IOException
    {
        writer.append(variableName + "=" + threeDPformatter.format(value) + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, String value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, SlicerType value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, FillPattern value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, SupportPattern value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputPrintCentre(FileWriter writer, float centreX, float centreY) throws IOException
    {
        outputLine(writer, "posx", (int) (centreX * 1000));
        outputLine(writer, "posy", (int) (centreY * 1000));
    }

    @Override
    protected void outputFilamentDiameter(FileWriter writer, float diameter) throws IOException
    {
        outputLine(writer, "filamentDiameter", String.format(Locale.UK, "%d", (int)(diameter * 1000)));
    }
}
