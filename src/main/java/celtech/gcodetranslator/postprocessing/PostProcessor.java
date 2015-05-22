package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import static org.parboiled.errors.ErrorUtils.printParseErrors;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import static org.parboiled.support.ParseTreeUtils.printNodeTree;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.ToStringFormatter;
import org.parboiled.trees.GraphNode;
import static org.parboiled.trees.GraphUtils.printTree;

/**
 *
 * @author Ian
 */
public class PostProcessor
{

    private Stenographer steno = StenographerFactory.getStenographer(PostProcessor.class.getName());
    private final String gcodeFileToProcess;
    private final String gcodeOutputFile;
    private final GCodeParser gcodeParser;

    public PostProcessor(String gcodeFileToProcess, String gcodeOutputFile)
    {
        this.gcodeFileToProcess = gcodeFileToProcess;
        this.gcodeOutputFile = gcodeOutputFile;

        gcodeParser = new GCodeParser();
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
                    // Parse the last layer if it exists...
                    if (layerBuffer.length() > 0)
                    {
                        ParsingResult<?> result = new TracingParseRunner(gcodeParser.Layer()).run(layerBuffer.toString());

                        if (result.hasErrors())
                        {
                            System.out.println("\nParse Errors:\n" + printParseErrors(result));
                        }

                        Object value = result.parseTreeRoot.getValue();
                        if (value != null)
                        {
                            String str = value.toString();
                            int ix = str.indexOf('|');
                            if (ix >= 0)
                            {
                                str = str.substring(ix + 2); // extract value part of AST node toString()
                            }
                            System.out.println(" = " + str + '\n');
                        }
                        if (value instanceof GraphNode)
                        {
                            System.out.println("\nAbstract Syntax Tree:\n"
                                    + printTree((GraphNode) value, new ToStringFormatter(null)) + '\n');
                        } else
                        {
                            System.out.println("\nParse Tree:\n" + printNodeTree(result) + '\n');
                        }
                    }

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
        } catch (IOException ex)
        {
            steno.error("Error reading post-processor input file: " + gcodeFileToProcess);
        }
    }
}
