/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.printing;

import celtech.printerControl.model.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.utils.SystemUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import javafx.beans.property.IntegerProperty;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class GCodePrinterTask extends Task<GCodePrintResult>
{

    private Printer printerToUse = null;
    private String gcodeFileToPrint = null;
    private String printJobID = null;
    private final Stenographer steno = StenographerFactory.getStenographer(this.getClass().getName());
    private IntegerProperty linesInFile = null;
    private boolean printUsingSDCard = true;
    private boolean isMacro = false;

    /**
     *
     * @param printerToUse
     * @param modelFileToPrint
     * @param printJobID
     * @param linesInFile
     * @param printUsingSDCard
     * @param isMacro
     */
    public GCodePrinterTask(Printer printerToUse, String modelFileToPrint, String printJobID, IntegerProperty linesInFile, boolean printUsingSDCard, boolean isMacro)
    {
        this.printerToUse = printerToUse;
        this.gcodeFileToPrint = modelFileToPrint;
        this.printJobID = printJobID;
        this.linesInFile = linesInFile;
        this.printUsingSDCard = printUsingSDCard;
        this.isMacro = isMacro;
    }

    @Override
    protected GCodePrintResult call() throws Exception
    {
        GCodePrintResult result = new GCodePrintResult();
        result.setPrintJobID(printJobID);

        boolean gotToEndOK = false;

        updateTitle("GCode Print ID:" + printJobID);
        int sequenceNumber = 0;
        boolean successfulWrite = false;
        File gcodeFile = new File(gcodeFileToPrint);
        FileReader gcodeReader = null;
        Scanner scanner = null;
        int numberOfLines = SystemUtils.countLinesInFile(gcodeFile, ";");
        linesInFile.setValue(numberOfLines);

        steno.info("Start of file " + gcodeFileToPrint);

        //Note that FileReader is used, not File, since File is not Closeable
        try
        {
            gcodeReader = new FileReader(gcodeFile);
            scanner = new Scanner(gcodeReader);

            if (printUsingSDCard)
            {
                printerToUse.initialiseDataFileSend(printJobID);
            }
            updateMessage("Transferring data");

            int lineCounter = 0;

            final int bufferSize = 512;
            StringBuffer outputBuffer = new StringBuffer(bufferSize);

            while (scanner.hasNextLine() && !isCancelled())
            {
                String line = scanner.nextLine();
                line = line.trim();

                boolean lineIngested = false;
                int positionInLine = 0;

                if (line.equals("") == false && line.startsWith(";") == false)
                {
                    line = SystemUtils.cleanGCodeForTransmission(line);
                    if (printUsingSDCard)
                    {
//                        steno.info("Sending line " + lineCounter);
                        printerToUse.sendDataFileChunk(line, lineCounter == numberOfLines - 1, true);
                        if ((printerToUse.getDataFileSequenceNumber() > 1 && printerToUse.isPrintInitiated() == false)
                                || (lineCounter == numberOfLines - 1 && printerToUse.isPrintInitiated() == false))
                        {
                            //Start printing!
                            printerToUse.initiatePrint(printJobID);
                        }
                    } else
                    {
                        printerToUse.sendRawGCode(line, false);
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
                printerToUse.abort();
            }
            updateMessage("Printing error");
        } finally
        {
            if (scanner != null)
            {
                scanner.close();
            }
            
            if (gcodeReader != null)
            {
                gcodeReader.close();
            }
        }

        result.setSuccess(gotToEndOK);
        result.setIsMacro(isMacro);
        return result;
    }

}
