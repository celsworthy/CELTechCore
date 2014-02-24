/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
class StreamGobbler extends Thread
{

    private InputStream is;
    private String type;
    private Stenographer steno = StenographerFactory.getStenographer(this.getClass().getName());

    StreamGobbler(InputStream is, String type)
    {
        this.is = is;
        this.type = type;
        this.setName("StreamGobbler");
    }

    @Override
    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                steno.error("Error during slicer execution: " + line);
            }
        } catch (IOException ioe)
        {
            steno.error("Error during slicer execution: " + ioe.toString());
        }
    }
}
