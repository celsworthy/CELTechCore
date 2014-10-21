package celtech.configuration.slicer;

import celtech.configuration.SlicerType;
import java.io.File;
import java.io.IOException;
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
        FileUtils.writeStringToFile(outputFile, variableName + " = " + value + "\n", true);
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
        FileUtils.writeStringToFile(outputFile, variableName + " = " + value + "\n", true);
    }

    @Override
    protected void outputLine(File outputFile, String variableName, SupportPattern value) throws IOException
    {
        FileUtils.writeStringToFile(outputFile, variableName + " = " + value + "\n", true);
    }
}
