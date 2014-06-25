/*
 * Copyright 2014 CEL UK
 */
package celtech.gcodetranslator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 *
 * @author tony
 */
public class PrintJobStatistics
{

    private final int numberOfLines;
    private final double volumeUsed;
    private final List<Integer> layerNumberToLineNumber;
    private final List<Double> layerNumberToPredictedDuration;
    private final int lineNumberOfFirstExtrusion;

    public PrintJobStatistics()
    {
        numberOfLines = 0;
        volumeUsed = 0;
        lineNumberOfFirstExtrusion = 0;
        layerNumberToLineNumber = null;
        layerNumberToPredictedDuration = null;
    }

    public PrintJobStatistics(int numberOfLines, 
        double volumeUsed,
        int lineNumberOfFirstExtrusion,
        List<Integer> layerNumberToLineNumber,
        List<Double> layerNumberToPredictedDuration)
    {
        this.numberOfLines = numberOfLines;
        this.volumeUsed = volumeUsed;
        this.lineNumberOfFirstExtrusion = lineNumberOfFirstExtrusion;
        this.layerNumberToLineNumber = layerNumberToLineNumber;
        this.layerNumberToPredictedDuration = layerNumberToPredictedDuration;
    }

    public void writeToFile(String statisticsFileLocation) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        mapper.writeValue(new File(statisticsFileLocation), this);
    }

    /**
     * @return the numberOfLines
     */
    public int getNumberOfLines()
    {
        return numberOfLines;
    }

    /**
     * @return the volumeUsed
     */
    public double getVolumeUsed()
    {
        return volumeUsed;
    }

    /**
     * @return the layerNumberToLineNumber
     */
    public List<Integer> getLayerNumberToLineNumber()
    {
        return layerNumberToLineNumber;
    }

    /**
     * @return the layerNumberToPredictedDuration
     */
    public List<Double> getLayerNumberToPredictedDuration()
    {
        return layerNumberToPredictedDuration;
    }

    /**
     * @return the lineNumberOfFirstExtrusion
     */
    public int getLineNumberOfFirstExtrusion()
    {
        return lineNumberOfFirstExtrusion;
    }

    /**
     * Create a PrintJobStatistics and populate it from a saved file
     * @param absolutePath the path of the file to load
     * @return
     * @throws IOException 
     */
    public static PrintJobStatistics readFromFile(String absolutePath) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        PrintJobStatistics printJobStatistics = mapper.readValue(new File(
            absolutePath), PrintJobStatistics.class);
        return printJobStatistics;
    }

}
