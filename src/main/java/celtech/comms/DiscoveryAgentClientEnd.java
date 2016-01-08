package celtech.comms;

import celtech.comms.remote.DiscoveryResponse;
import celtech.configuration.ApplicationConfiguration;
import celtech.printerControl.comms.DeviceDetector;
import celtech.printerControl.comms.PrinterStatusConsumer;
import java.io.IOException;
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
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author Ian
 */
public class DiscoveryAgentClientEnd implements Runnable, DeviceDetector
{
    
    private final Stenographer steno = StenographerFactory.getStenographer("DiscoveryAgentClientEnd");
    private final PrinterStatusConsumer printerStatusConsumer;
    private boolean initialised = false;
    private InetAddress group = null;
    private DatagramSocket s = null;
    private boolean keepRunning = true;
    private List<DeviceDetector.DetectedPrinter> currentPrinters = new ArrayList<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public DiscoveryAgentClientEnd(PrinterStatusConsumer printerStatusConsumer)
    {
        this.printerStatusConsumer = printerStatusConsumer;
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
                initialised = true;
            } catch (IOException ex)
            {
                steno.error("Unable to set up remote discovery client");
            }
        }
        
        while (keepRunning && initialised)
        {
            Set<InetAddress> serverAddresses = new HashSet<>();
            
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
                }
                
                List<DeviceDetector.DetectedPrinter> newlyDetectedPrinters = searchForDevices(serverAddresses);

                //Deal with disconnections
                currentPrinters.forEach(existingPrinter ->
                {
                    if (!newlyDetectedPrinters.contains(existingPrinter))
                    {
                        printerStatusConsumer.disconnected(existingPrinter);
                        currentPrinters.remove(existingPrinter);
                    }
                });

                //Now disconnections
                newlyDetectedPrinters.forEach(newPrinter ->
                {
                    if (!currentPrinters.contains(newPrinter))
                    {
                        currentPrinters.add(newPrinter);
                        printerStatusConsumer.printerConnected(newPrinter);
                    }
                });

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
                Thread.sleep(1500);
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
    
    private List<DetectedPrinter> searchForDevices(Set<InetAddress> serverAddresses)
    {
        List<DetectedPrinter> foundPrinters = new ArrayList<>();
        
        for (InetAddress address : serverAddresses)
        {
            String url = "http://" + address.getHostAddress() + ":9000/discovery";
            
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
                    DiscoveryResponse discoveryResponse = mapper.readValue(con.getInputStream(), DiscoveryResponse.class);
                    discoveryResponse.getPrinterIDs().forEach(printerID ->
                    {
                        DeviceDetector.RemoteDetectedPrinter remotePrinter = new RemoteDetectedPrinter(address, PrinterConnectionType.ROBOX_REMOTE, printerID);
                        foundPrinters.add(remotePrinter);
                    });
                    
//                    steno.info("Got response from @ " + address.getHostAddress() + " : " + discoveryResponse.toString());
                } else
                {
                    steno.warning("No response from @ " + address.getHostAddress());
                }
            } catch (IOException ex)
            {
                steno.error("Error whilst polling for remote printers @ " + address.getHostAddress());
            }
            
        }
        
        return foundPrinters;
    }
    
    @Override
    public List<DeviceDetector.DetectedPrinter> searchForDevices()
    {
        return currentPrinters;
    }
    
    @Override
    public void shutdownDetector()
    {
    }
    
}
