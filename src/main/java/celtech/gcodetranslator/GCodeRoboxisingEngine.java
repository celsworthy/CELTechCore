package celtech.gcodetranslator;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.SlicerType;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.events.MovementEvent;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.MacroLoadException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javafx.beans.property.DoubleProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public abstract class GCodeRoboxisingEngine implements GCodeTranslationEventHandler
{

    private final Stenographer steno = StenographerFactory
            .getStenographer(GCodeTranslationEventHandler.class.getName());

    private final Pattern passThroughPattern = Pattern
            .compile("\\b(?:M106 S[0-9.]+|M107|G[0-9]{1,}.*|M[0-9]{2,})(?:[\\s]*;.*)?");

    private final Pattern removePattern = Pattern
            .compile(
                    "\\b(?:M104 S[0-9.]+(?:\\sT[0-9]+)?|M109 S[0-9.]+(?:\\sT[0-9]+)?|M107)(?:[\\s]*;.*)?");

    protected GCodeOutputWriter outputWriter;
    private final GCodeFileParser gcodeParser = new GCodeFileParser();
    protected SlicerParametersFile currentSettings = null;

    protected double totalExtrudedVolume = 0;
    protected double totalXYMovement = 0;

    protected int lineNumberOfFirstExtrusion = -1;
    protected List<Integer> layerNumberToLineNumber;
    protected List<Double> layerNumberToDistanceTravelled;
    protected List<Double> layerNumberToPredictedDuration;
    protected double predictedDurationInLayer = 0.0;

    protected double volumeUsed = 0.0;
    protected double distanceSoFar = 0;
    protected double unretractFeedRate = 0;

    protected Vector2D lastPoint = null;
    protected Vector2D nozzleLastOpenedAt = null;
    protected MovementEvent lastProcessedMovementEvent = null;

    protected int layer = 0;
    protected double currentZHeight = 0;
    protected int forcedNozzleOnFirstLayer = -1;
    protected boolean nozzleHasBeenForced = false;

    protected boolean subsequentLayersTemperaturesWritten = false;

    protected SlicerType slicerType;

    protected ArrayList<NozzleProxy> nozzleProxies = null;
    protected NozzleProxy currentNozzle = null;
    // This counter is used to determine when to reselect the nozzle in use
    // When printing with a single nozzle it is possible for the home to be lost after many closes
    protected int triggerNozzleReselectAfterNCloses = 0;

    protected int wipeFeedRate_mmPerMin = 0;

    public GCodeRoboxisingEngine()
    {
        gcodeParser.addListener(this);
    }

    /**
     *
     * @param inputFilename
     * @param outputFilename
     * @param settings
     * @param percentProgress
     * @return
     */
    public final RoboxiserResult roboxiseFile(String inputFilename,
            String outputFilename,
            SlicerParametersFile settings, DoubleProperty percentProgress)
    {
        RoboxiserResult result = new RoboxiserResult();
        result.setSuccess(false);

        if (initialise(settings, outputFilename))
        {
            boolean success = false;

            try
            {
                SimpleDateFormat formatter = new SimpleDateFormat("EEE d MMM y HH:mm:ss", Locale.UK);
                outputWriter.writeOutput("; File post-processed by the CEL Tech Roboxiser on "
                        + formatter.format(new Date()) + "\n");
                outputWriter.
                        writeOutput("; " + ApplicationConfiguration.getTitleAndVersion() + "\n");

                outputWriter.writeOutput(";\n; Pre print gcode\n");
                for (String macroLine : GCodeMacros.getMacroContents("before_print"))
                {
                    outputWriter.writeOutput(macroLine + "\n");
                }
                outputWriter.writeOutput("; End of Pre print gcode\n");

                gcodeParser.parse(inputFilename, percentProgress, slicerType);

                outputWriter.close();

                steno.info("Finished roboxising " + inputFilename);
                steno.info("Total extrusion volume " + totalExtrudedVolume + " mm3");
                steno.info("Total XY movement distance " + totalXYMovement + " mm");

                success = true;
            } catch (IOException ex)
            {
                steno.error("Error roboxising file " + inputFilename);
            } catch (MacroLoadException ex)
            {
                steno.error(
                        "Error roboxising file - couldn't add before print header due to circular macro reference "
                        + inputFilename + " " + ex);
            }

            result.setSuccess(success);
            
            /**
             * TODO: layerNumberToLineNumber uses lines numbers from the GCode
             * file so are a little less than the line numbers for each layer
             * after roboxisation. As a quick fix for now set the line number of
             * the last layer to the actual maximum line number.
             */
            layerNumberToLineNumber.set(layerNumberToLineNumber.size() - 1, outputWriter.
                    getNumberOfLinesOutput());
            int numLines = outputWriter.getNumberOfLinesOutput();
            PrintJobStatistics roboxisedStatistics = new PrintJobStatistics(
                    numLines,
                    volumeUsed,
                    0,
                    lineNumberOfFirstExtrusion,
                    layerNumberToLineNumber, layerNumberToPredictedDuration);

            result.setRoboxisedStatistics(roboxisedStatistics);
        }

        return result;

    }

    protected final boolean initialise(SlicerParametersFile settings, String outputFilename)
    {
        boolean initialised = false;

        currentSettings = settings;

        if (currentSettings.getSlicerOverride() != null)
        {
            slicerType = currentSettings.getSlicerOverride();
        } else
        {
            slicerType = Lookup.getUserPreferences().getSlicerType();
        }

        layerNumberToLineNumber = new ArrayList<>();
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToDistanceTravelled.add(0, 0d);
        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToLineNumber.add(0, 0);

//        extruderMixPoints.clear();
//
//        extruderMixPoints.add(
//            new ExtruderMix(1, 0, 5));
//        extruderMixPoints.add(
//            new ExtruderMix(0, 1, 30));
//        extruderMixPoints.add(
//            new ExtruderMix(0.5, 0.5, 31));
//        extruderMixPoints.add(
//            new ExtruderMix(0.5, 0.5, 40));
//        extruderMixPoints.add(
//            new ExtruderMix(1, 0, 46));
//
//        if (mixExtruderOutputs
//            && extruderMixPoints.size()
//            >= 2)
//        {
//            ExtruderMix firstMixPoint = extruderMixPoints.get(0);
//            startingEMixValue = firstMixPoint.getEFactor();
//            startingDMixValue = firstMixPoint.getDFactor();
//            mixFromLayer = firstMixPoint.getLayerNumber();
//
//            ExtruderMix secondMixPoint = extruderMixPoints.get(1);
//            endEMixValue = secondMixPoint.getEFactor();
//            endDMixValue = secondMixPoint.getDFactor();
//            mixToLayer = secondMixPoint.getLayerNumber();
//        } else
//        {
//            mixExtruderOutputs = false;
//        }
        predictedDurationInLayer = 0.0;

        lastPoint = new Vector2D(0, 0);
        nozzleLastOpenedAt = new Vector2D(0, 0);

        subsequentLayersTemperaturesWritten = false;
        distanceSoFar = 0;
        totalExtrudedVolume = 0;
        totalXYMovement = 0;
        layer = 0;
        unretractFeedRate = 0;
        currentZHeight = 0;

        forcedNozzleOnFirstLayer = settings.getFirstLayerNozzle();

        nozzleProxies = new ArrayList<NozzleProxy>();

        for (int nozzleIndex = 0; nozzleIndex < settings.getNozzleParameters().size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(settings.getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        wipeFeedRate_mmPerMin = currentSettings.getPerimeterSpeed_mm_per_s() * 60;

        try
        {
            outputWriter = Lookup.getPostProcessorOutputWriterFactory().create(outputFilename);
            initialised = true;
        } catch (IOException ex)
        {
            steno.error("Failed to initialise post processor");
            ex.printStackTrace();
        }

        triggerNozzleReselectAfterNCloses = settings.getMaxClosesBeforeNozzleReselect();

        return initialised;
    }

    protected void processUnparsedLine(String line)
    {
        try
        {
            if ((removePattern.matcher(line)).matches())
            {
                steno.info("Removed " + line);
                outputWriter.writeOutput("; Removed: " + line);
            } else if ((passThroughPattern.matcher(line)).matches())
            {
                outputWriter.writeOutput(line);
                outputWriter.newLine();
            } else
            {
                steno.warning("Unable to parse " + line);
                outputWriter.writeOutput("; >>>ERROR PARSING: " + line);
                outputWriter.newLine();
            }
        } catch (IOException ex)
        {
            steno.error("Parse error - " + line);
        }

    }
}
