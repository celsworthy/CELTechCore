package celtech.utils;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.controllers.MyMiniFactoryLoaderController;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Ian
 */
public class MyMiniFactoryLoader extends Task<MyMiniFactoryLoadResult>
{
    
    private static final Stenographer steno = StenographerFactory.getStenographer(MyMiniFactoryLoader.class.getName());
    
    private String fileURL = null;
    
    public MyMiniFactoryLoader(String fileURL)
    {
        this.fileURL = fileURL;
    }
    
    @Override
    protected MyMiniFactoryLoadResult call() throws Exception
    {
        MyMiniFactoryLoadResult result = new MyMiniFactoryLoadResult();
        
        steno.info("Got download URL of " + fileURL);
        
        String tempID = SystemUtils.generate16DigitID();
        try
        {
            URL downloadURL = new URL(fileURL);
            
            String extension = FilenameUtils.getExtension(fileURL);
            final String tempFilename = ApplicationConfiguration.getApplicationStorageDirectory() + File.separator + tempID + "." + extension;
            
            URLConnection urlConn = downloadURL.openConnection();
            
            String file = fileURL.replaceFirst(".*/", "");
            
            InputStream webInputStream = urlConn.getInputStream();
            
            if (extension.equalsIgnoreCase("stl"))
            {
                steno.info("Got stl file from My Mini Factory");
                final String targetname = ApplicationConfiguration.getUserStorageDirectory() + File.separator + file;
                writeStreamToFile(webInputStream, targetname);
                final List<File> filesToLoad = new ArrayList<>();
                filesToLoad.add(new File(targetname));
                result.setFilesToLoad(filesToLoad);
                result.setSuccess(true);
            } else if (extension.equalsIgnoreCase("zip"))
            {
                steno.info("Got zip file from My Mini Factory");
                writeStreamToFile(webInputStream, tempFilename);
                ZipFile zipFile = new ZipFile(tempFilename);
                
                try
                {
                    final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    final List<File> filesToLoad = new ArrayList<>();
                    while (entries.hasMoreElements())
                    {
                        final ZipEntry entry = entries.nextElement();
                        final String tempTargetname = ApplicationConfiguration.getUserStorageDirectory() + File.separator + entry.getName();
                        writeStreamToFile(zipFile.getInputStream(entry), tempTargetname);
                        filesToLoad.add(new File(tempTargetname));
                    }
                    result.setFilesToLoad(filesToLoad);
                    result.setSuccess(true);
                } finally
                {
                    zipFile.close();
                }
            } else if (extension.equalsIgnoreCase("rar"))
            {
                steno.info("Got rar file from My Mini Factory");
                File f = new File(tempFilename);
                Archive a = null;
                try
                {
                    a = new Archive(new FileVolumeManager(f));
                } catch (RarException e)
                {
// TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e)
                {
// TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (a != null)
                {
                    a.getMainHeader().print();
                    FileHeader fh = a.nextFileHeader();
                    final List<File> filesToLoad = new ArrayList<>();
                    boolean ok = true;
                    while (fh != null && ok == true)
                    {
                        try
                        {
                            File out = new File(ApplicationConfiguration.getUserStorageDirectory()
                                + fh.getFileNameString().trim());
                            System.out.println(out.getAbsolutePath());
                            FileOutputStream os = new FileOutputStream(out);
                            a.extractFile(fh, os);
                            os.close();
                            filesToLoad.add(out);
                        } catch (FileNotFoundException e)
                        {
// TODO Auto-generated catch block
                            e.printStackTrace();
                            ok = false;
                        } catch (RarException e)
                        {
// TODO Auto-generated catch block
                            e.printStackTrace();
                            ok = false;
                        } catch (IOException e)
                        {
// TODO Auto-generated catch block
                            e.printStackTrace();
                            ok = false;
                        }
                        fh = a.nextFileHeader();
                    }
                    if (ok)
                    {
                        result.setFilesToLoad(filesToLoad);
                        result.setSuccess(true);
                    } else
                    {
                        result.setSuccess(false);
                    }
                }
            }
            
            webInputStream.close();
            
        } catch (IOException ex)
        {
            steno.error("Failed to download My Mini Factory file :" + fileURL);
            result.setSuccess(false);
        }
        
        return result;
    }
    
    private void writeStreamToFile(InputStream is, String localFilename) throws IOException
    {
        FileOutputStream fos = null;
        
        try
        {
            fos = new FileOutputStream(localFilename);   //open outputstream to local file

            byte[] buffer = new byte[4096];              //declare 4KB buffer
            int len;

            //while we have availble data, continue downloading and storing to local file
            while ((len = is.read(buffer)) > 0)
            {
                fos.write(buffer, 0, len);
            }
        } finally
        {
            try
            {
                if (is != null)
                {
                    is.close();
                }
            } finally
            {
                if (fos != null)
                {
                    fos.close();
                }
            }
        }
    }
}
