/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

/**
 *
 * @author ianhudson
 */
public class SlicedPrintJobIdentifier
{
    private int linesInPrintFile = 0;
    private String printJobUUID = null;

    public SlicedPrintJobIdentifier(int linesInPrintFile, String printJobUUID)
    {
        this.linesInPrintFile = linesInPrintFile;
        this.printJobUUID = printJobUUID;
    }

    public int getLinesInPrintFile()
    {
        return linesInPrintFile;
    }

    public void setLinesInPrintFile(int linesInPrintFile)
    {
        this.linesInPrintFile = linesInPrintFile;
    }

    public String getPrintJobUUID()
    {
        return printJobUUID;
    }

    public void setPrintJobUUID(String printJobUUID)
    {
        this.printJobUUID = printJobUUID;
    }
}
