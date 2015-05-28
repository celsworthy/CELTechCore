package celtech.printerControl.model;

/**
 *
 * @author ianhudson
 */
public class GCodeConstants
{

    /**
     *
     */
    protected static final String extruderRelativeMoveMode = "M83";

    /**
     *
     */
    protected static final String carriageRelativeMoveMode = "G91";

    /**
     *
     */
    protected static final String carriageAbsoluteMoveMode = "G90";

    /**
     *
     */
    protected static final String setAmbientTemperature = "M170 S";

    /**
     *
     */
    protected static final String setFirstLayerBedTemperatureTarget = "M139 S";

    /**
     *
     */
    protected static final String setBedTemperatureTarget = "M140 S";

    /**
     *
     */
    protected static final String goToTargetFirstLayerBedTemperature = "M139";

    /**
     *
     */
    protected static final String goToTargetBedTemperature = "M140";

    /**
     *
     */
    protected static final String waitForBedTemperature = "M190";

    /**
     *
     */
    protected static final String switchBedHeaterOff = "M140 S0";

    /**
     *
     */
    protected static final String setFirstLayerNozzleHeaterTemperatureTarget0 = "M103 S";
     protected static final String setFirstLayerNozzleHeaterTemperatureTarget1 = "M103 T";

    /**
     *
     */
    protected static final String setNozzleHeaterTemperatureTarget0 = "M104 S";
    protected static final String setNozzleHeaterTemperatureTarget1 = "M104 T";

    /**
     *
     */
    protected static final String goToTargetFirstLayerNozzleHeaterTemperature0 = "M103";
    protected static final String goToTargetFirstLayerNozzleHeaterTemperature1 = "M103 T";

    /**
     *
     */
    protected static final String goToTargetNozzleHeaterTemperature0 = "M104";
    protected static final String goToTargetNozzleHeaterTemperature1 = "M104 T";

    /**
     *
     */
    protected static final String waitForNozzleHeaterTemperature0 = "M109";
    protected static final String waitForNozzleHeaterTemperature1 = "M109 T";

    /**
     *
     */
    protected static final String switchNozzleHeaterOff0 = "M104 S0";
    protected static final String switchNozzleHeaterOff1 = "M104 T0";

    /**
     *
     */
    protected static final String homeXAxis = "G28 X";

    /**
     *
     */
    protected static final String homeYAxis = "G28 Y";

    /**
     *
     */
    protected static final String homeZAxis = "G28 Z";

    /**
     *
     */
    protected static final String probeZAxis = "G28 Z?";

    /**
     *
     */
    protected static final String queryZHomeDelta = "M113";

    /**
     *
     */
    protected static final String switchOnHeadLEDs = "M129";

    /**
     *
     */
    protected static final String switchOffHeadLEDs = "M128";

    /**
     *
     */
    protected static final String homeNozzle = "G28 B";

    /**
     *
     */
    protected static final String closeNozzle = "G0 B0";

    /**
     *
     */
    protected static final String openNozzle = "G0 B1";
    
    /**
     *
     */
    protected static final String openNozzleExtra = "G0 B2";

    /**
     *
     */
    protected static final String extrude = "G1 E ";

    /**
     *
     */
    protected static final String selectNozzle = "T";

    /**
     *
     */
    protected static final String disableStepperMotorsUntilNextMove = "M84";

    /**
     *
     */
    protected static final String ejectFilament = "M121";

    /**
     *
     */
    protected static final String setHeadFanSpeed = "M106 S";

    /**
     *
     */
    protected static final String switchOnHeadFan = "M106";

    /**
     *
     */
    protected static final String switchOffHeadFan = "M107";

    /**
     *
     */
    protected static final String goToOpenDoorPosition = "G37";
    
    /**
     * Don't wait for safe temp before opening door.
     */
    protected static final String goToOpenDoorPositionDontWait = "G37 S";
}
