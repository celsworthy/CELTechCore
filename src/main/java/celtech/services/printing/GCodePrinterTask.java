/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.printing;

import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.utils.SystemUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Scanner;
import javafx.beans.property.IntegerProperty;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class GCodePrinterTask extends Task<Boolean>
{

    private Printer printerToUse = null;
    private String gcodeFileToPrint = null;
    private String printJobID = null;
    private final Stenographer steno = StenographerFactory.getStenographer(this.getClass().getName());
    private IntegerProperty linesInFile = null;
    private boolean printUsingSDCard = true;

    public GCodePrinterTask(Printer printerToUse, String modelFileToPrint, String printJobID, IntegerProperty linesInFile, boolean printUsingSDCard)
    {
        this.printerToUse = printerToUse;
        this.gcodeFileToPrint = modelFileToPrint;
        this.printJobID = printJobID;
        this.linesInFile = linesInFile;
        this.printUsingSDCard = printUsingSDCard;
    }

    @Override
    protected Boolean call() throws Exception
    {
        boolean gotToEndOK = false;

        updateTitle("GCode Print ID:" + printJobID);
        int sequenceNumber = 0;
        boolean successfulWrite = false;
        File gcodeFile = new File(gcodeFileToPrint);
        int numberOfLines = SystemUtils.countLinesInFile(gcodeFile, ";");
        linesInFile.setValue(numberOfLines);

        steno.info("Start of file " + gcodeFileToPrint);

        //Note that FileReader is used, not File, since File is not Closeable
        try
        {
            if (printUsingSDCard)
            {
                printerToUse.initialiseDataFileSend(printJobID);
            }
            updateMessage("Transferring data");

            Scanner scanner = new Scanner(new FileReader(gcodeFile));
            int lineCounter = 0;

            final int bufferSize = 512;
            StringBuffer outputBuffer = new StringBuffer(bufferSize);

            while (scanner.hasNextLine() && !isCancelled())
            {
                String line = scanner.nextLine();
                line = line.replaceFirst(";.*", "");
                line = line.trim();

                boolean lineIngested = false;
                int positionInLine = 0;

                if (line.equals("") == false && line.startsWith(";") == false)
                {
                    if (printUsingSDCard)
                    {
                        printerToUse.sendDataFileChunk(line, lineCounter == numberOfLines - 1, true);
                        if (printerToUse.getSequenceNumber() > 1 && printerToUse.isPrintInitiated() == false)
                        {
                            //Start printing!
                            printerToUse.initiatePrint(printJobID);
                        }
                    } else
                    {
                        printerToUse.transmitDirectGCode(line, false);
                    }
                    lineCounter++;
                }
                if (lineCounter < numberOfLines)
                {
                    updateProgress(lineCounter, numberOfLines);
                }
            }
            gotToEndOK = true;
        } catch (FileNotFoundException ex)
        {
            steno.error("Couldn't open gcode file " + gcodeFileToPrint);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error during print operation - abandoning print " + printJobID + " " + ex.getMessage());
            if (printUsingSDCard)
            {
                printerToUse.abortPrint();
            }
            updateMessage("Printing error");
        }

        return gotToEndOK;
    }

}
