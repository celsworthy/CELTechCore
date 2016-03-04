package celtech.coreUI.visualisation;

/**
 *
 * @author Ian
 */
public interface ScreenExtentsProviderTwoD
{

    public ScreenExtents getScreenExtents();

    public double getTransformedHeight();

    public double getTransformedWidth();

    public void addScreenExtentsChangeListener(ScreenExtentsListener listener);

    public void removeScreenExtentsChangeListener(ScreenExtentsListener listener);

    public interface ScreenExtentsListener
    {

        public void screenExtentsChanged(ScreenExtentsProviderTwoD screenExtentsProvider);
    }
}
