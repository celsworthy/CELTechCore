package celtech.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class DiscoveryAgentRemoteEnd implements Runnable
{

//    private final Stenographer steno = StenographerFactory.getStenographer("DiscoveryAgentRemoteEnd");
    private boolean keepRunning = true;

    @Override
    public void run()
    {
        try
        {
            InetAddress group = InetAddress.getByName(RemoteDiscovery.multicastAddress);
            MulticastSocket s = new MulticastSocket(RemoteDiscovery.multicastSocket);
            s.joinGroup(group);
            while (keepRunning)
            {
                byte[] buf = new byte[1000];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                s.receive(recv);
                
                System.out.println("Got multicast from " + recv.getSocketAddress() + " content was " + recv.getLength() + " bytes");
            }

            s.leaveGroup(group);
        } catch (IOException ex)
        {
            System.out.println("Error listening for multicast messages");
        }
    }

    public void shutdown()
    {
        keepRunning = false;
    }
}
