package celtech.gcodetranslator.postprocessing;

import celtech.configuration.ApplicationConfiguration;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.MacroLoadException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author Ian
 */
public class OutputUtilities
{

    protected void prependPrePrintHeader(GCodeOutputWriter writer)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE d MMM y HH:mm:ss", Locale.UK);
        try
        {
            writer.writeOutput("; File post-processed by the CEL Tech Roboxiser on "
                    + formatter.format(new Date()) + "\n");
            writer.writeOutput("; " + ApplicationConfiguration.getTitleAndVersion() + "\n");

            writer.writeOutput(";\n; Pre print gcode\n");

            for (String macroLine : GCodeMacros.getMacroContents("before_print"))
            {
                writer.writeOutput(macroLine);
                writer.newLine();
            }

            writer.writeOutput("; End of Pre print gcode\n");
        } catch (IOException | MacroLoadException ex)
        {
            throw new RuntimeException("Failed to add pre-print header in post processor - " + ex.getMessage(), ex);
        }
    }

    protected void appendPostPrintFooter(GCodeOutputWriter writer, final float totalEVolume, final float totalDVolume, final double totalTimeInSecs)
    {
        try
        {
            writer.writeOutput(";\n; Post print gcode\n");
            for (String macroLine : GCodeMacros.getMacroContents("after_print"))
            {
                writer.writeOutput(macroLine);
                writer.newLine();
            }
            writer.writeOutput("; End of Post print gcode\n");
            writer.writeOutput(";\n");
            writer.writeOutput("; Volume of material - Extruder E - " + totalEVolume + "\n");
            writer.writeOutput("; Volume of material - Extruder D - " + totalDVolume + "\n");
            writer.writeOutput("; Total print time estimate - " + totalTimeInSecs + " seconds\n");
            writer.writeOutput(";\n");
        } catch (IOException | MacroLoadException ex)
        {
            throw new RuntimeException("Failed to add post-print footer in post processor - " + ex.getMessage(), ex);
        }
    }

    protected void outputTemperatureCommands(GCodeOutputWriter writer)
    {
        try
        {
            MCodeNode nozzleTemp = new MCodeNode(104);
            nozzleTemp.setCommentText("Go to nozzle temperature from loaded reel - don't wait");
            writer.writeOutput(nozzleTemp.renderForOutput());
            writer.newLine();

            MCodeNode bedTemp = new MCodeNode(140);
            bedTemp.setCommentText("Go to bed temperature from loaded reel - don't wait");
            writer.writeOutput(bedTemp.renderForOutput());
            writer.newLine();
        } catch (IOException ex)
        {
            throw new RuntimeException("Failed to add post layer 1 temperature commands in post processor - " + ex.getMessage(), ex);
        }
    }

    protected void writeLayerToFile(LayerNode layerNode, GCodeOutputWriter writer)
    {
        if (layerNode != null)
        {
            Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);

            while (layerIterator.hasNext())
            {
                GCodeEventNode node = layerIterator.next();

                if (node instanceof Renderable)
                {
                    Renderable renderableNode = (Renderable) node;
                    try
                    {
                        writer.writeOutput(renderableNode.renderForOutput());
                        writer.newLine();
                    } catch (IOException ex)
                    {
                        throw new RuntimeException("Error outputting post processed data at node " + renderableNode.renderForOutput(), ex);
                    }
                }
            }
        }
    }

    protected void outputNodes(GCodeEventNode node, int level)
    {
        //Output me
        StringBuilder outputBuilder = new StringBuilder();

        for (int levelCount = 0; levelCount < level; levelCount++)
        {
            outputBuilder.append('\t');
        }
        if (node instanceof Renderable)
        {
            outputBuilder.append(((Renderable) node).renderForOutput());
        } else
        {
            outputBuilder.append(node.toString());
        }
        System.out.println(outputBuilder.toString());

        //Output my children
        List<GCodeEventNode> children = node.getChildren();
        for (GCodeEventNode child : children)
        {
            level++;
            outputNodes(child, level);
            level--;
        }
    }
}
