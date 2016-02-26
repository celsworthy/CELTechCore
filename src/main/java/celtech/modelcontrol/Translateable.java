package celtech.modelcontrol;

/**
 *
 * @author ianhudson
 */
public interface Translateable
{

    public void translateBy(double xMove, double zMove);

    public void translateBy(double xMove, double yMove, double zMove);

    public void translateTo(double xPosition, double zPosition);

    public void translateXTo(double xPosition);

    public void translateZTo(double zPosition);

}
