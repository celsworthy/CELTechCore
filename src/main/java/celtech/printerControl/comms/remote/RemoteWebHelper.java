package celtech.printerControl.comms.remote;

import celtech.comms.remote.rx.RoboxRxPacket;
import celtech.configuration.ApplicationConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author Ian
 */
public class RemoteWebHelper
{

    private final static Stenographer steno = StenographerFactory.getStenographer(RemoteWebHelper.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void postData(String urlString)
    {
        postData(urlString, null, null);
    }

    public static RoboxRxPacket postData(String urlString, String content, Class<?> expectedResponseClass)
    {
        Object returnvalue = null;
        try
        {
            URL obj = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("POST");

            //add request header
            con.setRequestProperty("User-Agent", ApplicationConfiguration.getApplicationName());

            if (content != null)
            {
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Content-Length", "" + content.length());
                con.getOutputStream().write(content.getBytes());
            }

            con.setConnectTimeout(5000);
            int responseCode = con.getResponseCode();

            if (responseCode == 200)
            {
                if (expectedResponseClass != null)
                {
                    returnvalue = mapper.readValue(con.getInputStream(), expectedResponseClass);
                }
            } else
            {
                steno.warning("Got " + responseCode + " when trying " + urlString);
            }
        } catch (IOException ex)
        {
            steno.error("Error when attempting to connect to " + urlString + " : " + ex.getMessage());
        }

        return (RoboxRxPacket)returnvalue;
    }
}
