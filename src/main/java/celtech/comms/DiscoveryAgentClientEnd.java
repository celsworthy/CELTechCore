package celtech.comms;

import celtech.printerControl.comms.DeviceDetector;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private MulticastSocket s = null;
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
                s = new MulticastSocket(RemoteDiscovery.multicastSocket);
                s.joinGroup(group);
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
                        group, RemoteDiscovery.multicastSocket);

                s.send(hi);

                byte[] buf = new byte[1000];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                s.receive(recv);

                steno.info("Got multicast from " + recv.getAddress().getHostAddress() + ":" + recv.getSocketAddress() + " content was " + recv.getLength() + " bytes " + String.valueOf(recv.getData()));
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

        if (s.isConnected())
        {
            try
            {
                s.leaveGroup(group);
                s.close();
            } catch (IOException ex)
            {
                steno.error("Unable to leave remote host detection group");
            }
        }
    }

}
