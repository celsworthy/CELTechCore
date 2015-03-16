package celtech.utils.Time;

/**
 *
 * @author Ian
 */
public class TimeUtils
{
    public static String convertToHoursMinutes(int seconds)
    {
        int minutes = (int) (seconds / 60);
        int hours = minutes / 60;
        minutes = minutes - (60 * hours);
        return String.format("%02d:%02d", hours, minutes);
    }
}
