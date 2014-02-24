/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import java.io.File;
import java.io.FileFilter;

/**
 *
 * @author ianhudson
 */
public class HeadFileFilter implements FileFilter
{

    @Override
    public boolean accept(File pathname)
    {
        if (pathname.getName().endsWith(ApplicationConfiguration.headFileExtension))
        {
            return true;
        } else
        {
            return false;
        }
    }

}
