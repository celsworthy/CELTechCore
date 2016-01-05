package celtech.comms;

import celtech.configuration.ApplicationConfiguration;
import celtech.printerControl.comms.DeviceDetector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class DiscoveryAgentClientEnd implements Runnable, DeviceDetector
{

    private final Stenographer steno = StenographerFactory.getStenographer("DiscoveryAgentClientEnd");
    private boolean initialised = false;
    private InetAddress group = null;
    private DatagramSocket s = null;
    private final RemoteHostListener remoteHostListener;
    private boolean keepRunning = true;
    private Set<InetAddress> serverAddresses = new HashSet<>();

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

                if (Arrays.equals(Arrays.copyOf(buf, RemoteDiscovery.iAmHereMessage.getBytes("US-ASCII").length),
                        RemoteDiscovery.iAmHereMessage.getBytes("US-ASCII")))
                {
                    serverAddresses.add(recv.getAddress());
                    steno.info("Server added at:" + recv.getAddress());
                }
//                steno.info("Got response from " + recv.getAddress().getHostAddress() + ":" + recv.getSocketAddress() + " content was " + recv.getLength() + " bytes " + String.valueOf(recv.getData()));
            } catch (SocketTimeoutException ex)
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

    public void shutdown()
    {
        keepRunning = false;
    }

    @Override
    public List<DetectedPrinter> searchForDevices()
    {
        List<DetectedPrinter> detectedPrinters = new ArrayList<>();

        for (InetAddress address : serverAddresses)
        {
            String url = "http://" + address.getHostAddress() + ":9000/printerControl";

            try
            {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                // optional default is GET
                con.setRequestMethod("GET");

                //add request header
                con.setRequestProperty("User-Agent", ApplicationConfiguration.getApplicationName());

                con.setConnectTimeout(5000);
                int responseCode = con.getResponseCode();

                if (responseCode == 200)
                {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null)
                    {
                        response.append(inputLine);
                    }
                    in.close();

                    steno.info("Got response from @ " + address.getHostAddress() + " : " + response.toString());
                } else
                {
                    steno.warning("No response from @ " + address.getHostAddress());
                }
            } catch (IOException ex)
            {
                steno.error("Error whilst polling for remote printers @ " + address.getHostAddress());
            }

        }

        return detectedPrinters;
    }

    @Override
    public void shutdownDetector()
    {
    }

}
