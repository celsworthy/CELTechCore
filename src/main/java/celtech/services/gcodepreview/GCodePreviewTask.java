/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.gcodepreview;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.MachineType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Tony
 */
public class GCodePreviewTask extends Task<Boolean> {

    private static final Stenographer steno = StenographerFactory.getStenographer(GCodePreviewTask.class.getName());
    private OutputStream stdInStream;
    private final IntegerProperty layerCountProperty = new SimpleIntegerProperty(0);
    private List<String> pendingCommands = null;
    
    public GCodePreviewTask()
    {
        this.stdInStream = null;
    }

    public IntegerProperty getLayerCountProperty()
    {
        return layerCountProperty;
    }
    
    private void writeToInStream(String command) throws IOException
    {
        steno.info("Writing command \"" + command + "\"");
        stdInStream.write(command.getBytes());
        stdInStream.write('\n');
    }
    
    public synchronized void writeCommand(String command)
    {
        if (this.stdInStream == null) {
            if (pendingCommands == null)
                pendingCommands = new ArrayList<>();
            pendingCommands.add(command);
        }
        else
        {
            try {
                flushPendingCommands();
                writeToInStream(command);
                stdInStream.flush();
            }
            catch (IOException ex) {
                steno.warning("Failed to write command \"" + command + "\": " + ex.getMessage());
            }
        }
    }

    public synchronized void flushPendingCommands() throws IOException
    {
        if (pendingCommands != null)
        {
            for (int i = 0; i < pendingCommands.size(); ++i)
                writeToInStream(pendingCommands.get(i));
            pendingCommands = null;
        }
    }

    public void loadGCodeFile(String fileName)
    {
        StringBuilder command = new StringBuilder();
        command.append("load ");
        command.append(fileName);
        command.trimToSize();

        writeCommand(command.toString());
    }

    public void setToolColour(int toolIndex, Color colour)
    {
        StringBuilder command = new StringBuilder();
        command.append("colour tool ");
        command.append(Integer.toString(toolIndex));
        command.append(" ");
        command.append(Double.toString(colour.getRed()));
        command.append(" ");
        command.append(Double.toString(colour.getGreen()));
        command.append(" ");
        command.append(Double.toString(colour.getBlue()));
        command.trimToSize();

        writeCommand(command.toString());
    }

    public void setTopLayer(int topLayer)
    {
        StringBuilder command = new StringBuilder();
        command.append("top ");
        command.append(topLayer);
        command.trimToSize();

        writeCommand(command.toString());
    }

    public void setMovesVisible(boolean flag)
    {
        StringBuilder command = new StringBuilder();
        command.append("set moves ");
        if (flag)
            command.append("visible");
        else
            command.append("hidden");
        command.trimToSize();

        writeCommand(command.toString());
    }
    
    public void clearGCode()
    {
        writeCommand("clear");
    }

    public void terminatePreview()
    {
        if (this.stdInStream != null)
        {
            String command = "q";
            writeCommand(command.toString());
        }
    }

    @Override
    protected Boolean call() throws Exception {
        Boolean succeeded = false;
        ArrayList<String> commands = new ArrayList<>();
        
        commands.add("java");
        commands.add("-DlibertySystems.configFile=" + BaseConfiguration.getGCodeViewerDirectory() + "GCodeViewer.configFile.xml");
        commands.add("-jar");
        commands.add(BaseConfiguration.getGCodeViewerDirectory() + "GCodeViewer.jar");
        String languageTag = BaseConfiguration.getApplicationLocale();
        if (languageTag != null)
            commands.add("-l" + languageTag);

        if (commands.size() > 0)
        {
            steno.debug("GCodePreviewTask command is \"" + String.join(" ", commands) + "\"");
            ProcessBuilder previewProcessBuilder = new ProcessBuilder(commands);
            previewProcessBuilder.redirectErrorStream(true);

            Process previewProcess = null;
            try {
                previewProcess = previewProcessBuilder.start();

                GCodePreviewConsumer outputConsumer = new GCodePreviewConsumer(previewProcess.getInputStream());
                outputConsumer.setLayerCountProperty(layerCountProperty);
                synchronized(this){
                    this.stdInStream =  previewProcess.getOutputStream();
                    try {
                        flushPendingCommands();
                        stdInStream.flush();
                    }
                    catch (IOException ex) {
                        steno.warning("Failed to flush pending commands: " + ex.getMessage());
                    }
                }

                // Start output consumer.
                outputConsumer.start();
                
                int exitStatus = previewProcess.waitFor();
                switch (exitStatus)
                {
                    case 0:
                        steno.debug("GCode previewer terminated successfully ");
                        succeeded = true;
                        break;
                    default:
                        steno.error("Failure when invoking gcode previewer with command line: \"" + String.join(
                                " ", commands) + "\"");
                        steno.error("GCode Previewer terminated with exit code " + exitStatus);
                        break;
                }
            }
            catch (IOException ex) {
                steno.error("Exception whilst running gcode previewer: " + ex);
            } 
            catch (InterruptedException ex) {
                steno.warning("Interrupted whilst waiting for GCode Previewer to complete");
                if (previewProcess != null)
                {
                    previewProcess.destroyForcibly();
                }
            }
        } else
        {
            steno.error("Couldn't run GCode Previewer - no commands for OS ");
        }

        return succeeded;
    }
}
