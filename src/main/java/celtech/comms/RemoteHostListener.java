package celtech.comms;

/**
 *
 * @author Ian
 */
public interface RemoteHostListener
{
    public void hostAdded(RemotePrinterHost host);
    public void hostRemoved(RemotePrinterHost host);
}
