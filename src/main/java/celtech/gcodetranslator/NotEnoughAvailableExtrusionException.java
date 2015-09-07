package celtech.gcodetranslator;

/**
 *
 * @author Ian
 */
class NotEnoughAvailableExtrusionException extends Exception
{
    public NotEnoughAvailableExtrusionException(String exceptionInformation)
    {
        super(exceptionInformation);
    }
}
