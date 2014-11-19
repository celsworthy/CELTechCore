package celtech.gcodetranslator;

import java.io.IOException;

/**
 *
 * @author Ian
 * @param <T>
 */
public interface GCodeOutputWriterFactory<T extends GCodeOutputWriter>
{
    T create(String filename) throws IOException;
}
