package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.parboiled.Parboiled;
import static org.parboiled.errors.ErrorUtils.printParseErrors;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Ian
 */
public class PostProcessor
{

    private Stenographer steno = StenographerFactory.getStenographer(PostProcessor.class.getName());
    private final String gcodeFileToProcess;
    private final String gcodeOutputFile;

    public PostProcessor(String gcodeFileToProcess, String gcodeOutputFile)
    {
        this.gcodeFileToProcess = gcodeFileToProcess;
        this.gcodeOutputFile = gcodeOutputFile;
    }

    public void processInput()
    {
        //Cura has line delineators like this ';LAYER:1'
        try
        {
            BufferedReader fileReader = new BufferedReader(new FileReader(gcodeFileToProcess));
            StringBuilder layerBuffer = new StringBuilder();
            int layerCounter = 0;

            for (String lineRead = fileReader.readLine(); lineRead != null; lineRead = fileReader.readLine())
            {
                lineRead = lineRead.trim();
                if (lineRead.matches(";LAYER:[0-9]+"))
                {
                    parseLayer(layerBuffer);

                    layerCounter++;
                    layerBuffer = new StringBuilder();
                    // Make sure this layer command is at the start
                    layerBuffer.append(lineRead);
                    layerBuffer.append('\n');
                } else if (layerCounter == 0)
                {
                    //We're seeing data before the start of a layer - dump it for the moment
                } else
                {
                    // This must be data post first-layer - stash it in the buffer
                    layerBuffer.append(lineRead);
                    layerBuffer.append('\n');
                }
            }
            
            //This catches the last layer - if we had no data it won't do anything
            parseLayer(layerBuffer);
        } catch (IOException ex)
        {
            steno.error("Error reading post-processor input file: " + gcodeFileToProcess);
        }
    }

    private void parseLayer(StringBuilder layerBuffer)
    {
        // Parse the last layer if it exists...
        if (layerBuffer.length() > 0)
        {
            
            GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
            RecoveringParseRunner runner = new RecoveringParseRunner<>(gcodeParser.Layer());
            ParsingResult result = runner.run(layerBuffer.toString());
            
            if (result.hasErrors())
            {
                System.out.println("\nParse Errors:\n" + printParseErrors(result));
            } else
            {
                LayerNode layerNode = gcodeParser.getLayerNode();
                outputNodes(layerNode, 0);
            }
        }
    }

    private void outputNodes(GCodeEventNode node, int level)
    {
        //Output me
        System.out.println("Level - " + level + " " + node.renderForOutput());

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
