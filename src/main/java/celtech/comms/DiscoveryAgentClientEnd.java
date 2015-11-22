package celtech.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class DiscoveryAgentClientEnd implements Runnable
{

    private final Stenographer steno = StenographerFactory.getStenographer("DiscoveryAgentClientEnd");
    private boolean initialised = false;
    private InetAddress group = null;
    private DatagramSocket s = null;
    private final RemoteHostListener remoteHostListener;
    private boolean keepRunning = true;

    public void shutdown()
    {
        keepRunning = false;
    }

    public DiscoveryAgentClientEnd(RemoteHostListener listener)
    {
        this.remoteHostListener = listener;
    }

    @Override
    public void run()
    {
        if (!initialised)
        {
            try
            {
                group = InetAddress.getByName(RemoteDiscovery.multicastAddress);
                s = new DatagramSocket(RemoteDiscovery.clientSocket);
                s.setSoTimeout(500);
                steno.info("Bound to " + s.getLocalAddress());
                initialised = true;
            } catch (IOException ex)
            {
                steno.error("Unable to set up remote discovery client");
            }
        }

        while (keepRunning && initialised)
        {
            try
            {
                DatagramPacket hi = new DatagramPacket(RemoteDiscovery.discoverHostsMessage.getBytes("US-ASCII"),
                        RemoteDiscovery.discoverHostsMessage.length(),
                        group, RemoteDiscovery.remoteSocket);

                s.send(hi);

                byte[] buf = new byte[100];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                s.receive(recv);

                steno.info("Got response from " + recv.getAddress().getHostAddress() + ":" + recv.getSocketAddress() + " content was " + recv.getLength() + " bytes " + String.valueOf(recv.getData()));
            }
            catch (SocketTimeoutException ex)
            {
                //Nothing heard
            } catch (IOException ex)
            {
                steno.error("Unable to query for remote hosts");
            }

            try
            {
                Thread.sleep(500);
            } catch (InterruptedException ex)
            {
                steno.warning("Interrupted within remote host discovery loop");
            }
        }

    }

}
