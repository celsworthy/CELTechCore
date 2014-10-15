/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.printerControl.model.Printer;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class PrinterListChangesNotifierTest
{

//    @Test
    public void testWhenPrinterAdded()
    {
        ObservableList<Printer> printers = FXCollections.observableArrayList();
        PrinterListChangesNotifier notifier = new PrinterListChangesNotifier(printers);
        TestPrinterListChangesListener plcListener = new TestPrinterListChangesListener();
        notifier.addListener(plcListener);
        
        assertEquals(0, plcListener.addedPrinters.size());
        TestPrinter printer = new TestPrinter();
        printers.add(printer);
        assertEquals(1, plcListener.addedPrinters.size());
    }
    
//    @Test
    public void testWhenPrinterAddedAndRemoved()
    {
        ObservableList<Printer> printers = FXCollections.observableArrayList();
        PrinterListChangesNotifier notifier = new PrinterListChangesNotifier(printers);
        TestPrinterListChangesListener plcListener = new TestPrinterListChangesListener();
        notifier.addListener(plcListener);
        
        TestPrinter printer = new TestPrinter();
        printers.add(printer);
        printers.remove(printer);
        assertEquals(0, plcListener.addedPrinters.size());
    }   
    
    @Test
    public void testWhenPrinterAddedThenHeadAdded()
    {
        ObservableList<Printer> printers = FXCollections.observableArrayList();
        PrinterListChangesNotifier notifier = new PrinterListChangesNotifier(printers);
        TestPrinterListChangesListener plcListener = new TestPrinterListChangesListener();
        notifier.addListener(plcListener);
        
        assertEquals(0, plcListener.printersWithHeadAdded.size());
        TestPrinter printer = new TestPrinter();
        printers.add(printer);
        printer.addHead();
        assertEquals(1, plcListener.printersWithHeadAdded.size());
    }    

    private static class TestPrinterListChangesListener implements PrinterListChangesListener
    {
        
        public List<Printer> addedPrinters = new ArrayList<>();
        public List<Printer> printersWithHeadAdded = new ArrayList<>();

        @Override
        public void whenPrinterAdded(Printer printer)
        {
            addedPrinters.add(printer);
        }

        @Override
        public void whenPrinterRemoved(Printer printer)
        {
            addedPrinters.remove(printer);
        }

        @Override
        public void whenHeadAdded(Printer printer)
        {
            printersWithHeadAdded.add(printer);
        }

        @Override
        public void whenHeadRemoved(Printer printer)
        {

        }

        @Override
        public void whenReelAdded(Printer printer, int reelIndex)
        {

        }

        @Override
        public void whenReelRemoved(Printer printer, int reelIndex)
        {

        }

        @Override
        public void whenPrinterIdentityChanged(Printer printer)
        {

        }
    }


}
