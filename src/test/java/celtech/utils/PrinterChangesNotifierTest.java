/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class PrinterChangesNotifierTest
{

    @Test
    public void testWhenHeadAdded()
    {
        TestPrinter printer = new TestPrinter();
        PrinterChangesNotifier notifier = new PrinterChangesNotifier(printer);
        TestPrinterChangesListener listener = new TestPrinterChangesListener();
        notifier.addListener(listener);

        printer.addHead();

        assertTrue(listener.headAdded);
    }

    @Test
    public void testWhenHeadRemoved()
    {
        TestPrinter printer = new TestPrinter();
        PrinterChangesNotifier notifier = new PrinterChangesNotifier(printer);
        TestPrinterChangesListener listener = new TestPrinterChangesListener();
        notifier.addListener(listener);

        printer.addHead();
        printer.removeHead();

        assertTrue(listener.headAdded);
    }

    @Test
    public void testWhenReelAdded()
    {
        TestPrinter printer = new TestPrinter();
        PrinterChangesNotifier notifier = new PrinterChangesNotifier(printer);
        TestPrinterChangesListener listener = new TestPrinterChangesListener();
        notifier.addListener(listener);

        printer.addReel(0);

        assertTrue(listener.reel0Added);
    }
    
    @Test
    public void testWhenReelRemoved()
    {
        TestPrinter printer = new TestPrinter();
        PrinterChangesNotifier notifier = new PrinterChangesNotifier(printer);
        TestPrinterChangesListener listener = new TestPrinterChangesListener();
        notifier.addListener(listener);

        printer.addReel(0);
        printer.removeReel(0);

        assertTrue(listener.reel0Removed);
    }    

    private static class TestPrinterChangesListener implements PrinterChangesListener
    {

        public boolean headAdded = false;
        public boolean headRemoved = false;
        public boolean reel0Added = false;
        public boolean reel0Removed = false;

        @Override
        public void whenHeadAdded()
        {
            headAdded = true;
        }

        @Override
        public void whenHeadRemoved()
        {
            headRemoved = true;
        }

        @Override
        public void whenReelAdded(int reelIndex)
        {
            reel0Added = true;
        }

        @Override
        public void whenReelRemoved(int reelIndex)
        {
            reel0Removed = true;
        }

        @Override
        public void whenPrinterIdentityChanged()
        {
        }
    }

}
