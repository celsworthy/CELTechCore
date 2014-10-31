package celtech.configuration.fileRepresentation;

import celtech.configuration.SlicerType;
import celtech.configuration.slicer.FillPattern;
import celtech.configuration.slicer.NozzleParameters;
import celtech.configuration.slicer.SupportPattern;
import java.util.ArrayList;

/**
 *
 * @author Ian
 */
public class SlicerParameters
{

    private int version = 2;
    private String profileName;
    private SlicerType slicerOverride;

    /*
     * Extrusion data
     */
    private float firstLayerHeight_mm;
    private float layerHeight_mm;
    private float fillDensity_normalised;
    private FillPattern fillPattern;
    private int fillEveryNLayers;
    private int solidLayersAtTop;
    private int solidLayersAtBottom;
    private int numberOfPerimeters;
    private int brimWidth_mm;
    private boolean spiralPrint;

    /*
     * Nozzle data
     */
    private float firstLayerExtrusionWidth_mm;
    private float perimeterExtrusionWidth_mm;
    private float fillExtrusionWidth_mm;
    private float solidFillExtrusionWidth_mm;
    private float topSolidFillExtrusionWidth_mm;
    private float supportExtrusionWidth_mm;
    private ArrayList<NozzleParameters> nozzleParameters;
    private int firstLayerNozzle;
    private int perimeterNozzle;
    private int fillNozzle;
    private int supportNozzle;
    private int supportInterfaceNozzle;

    /*
     * Support
     */
    private boolean generateSupportMaterial;
    private int supportOverhangThreshold_degrees;
    private int forcedSupportForFirstNLayers;
    private SupportPattern supportPattern;
    private float supportPatternSpacing_mm;
    private int supportPatternAngle_degrees;

    /*
     * Speed settings
     */
    private int perimeterSpeed_mm_per_s;
    private int smallPerimeterSpeed_mm_per_s;
    private int externalPerimeterSpeed_mm_per_s;
    private int fillSpeed_mm_per_s;
    private int solidFillSpeed_mm_per_s;
    private int topSolidFillSpeed_mm_per_s;
    private int supportSpeed_mm_per_s;
    private int bridgeSpeed_mm_per_s;
    private int gapFillSpeed_mm_per_s;

    /*
     * Cooling
     */
    private boolean enableCooling;
    private int minFanSpeed_percent;
    private int maxFanSpeed_percent;
    private int bridgeFanSpeed_percent;
    private int disableFanFirstNLayers;
    private int coolIfLayerTimeLessThan_secs;
    private int slowDownIfLayerTimeLessThan_secs;
    private int minPrintSpeed_mm_per_s;

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public String getProfileName()
    {
        return profileName;
    }

    public void setProfileName(String profileName)
    {
        this.profileName = profileName;
    }

    public SlicerType getSlicerOverride()
    {
        return slicerOverride;
    }

    public void setSlicerOverride(SlicerType slicerOverride)
    {
        this.slicerOverride = slicerOverride;
    }

    public float getFirstLayerHeight_mm()
    {
        return firstLayerHeight_mm;
    }

    public void setFirstLayerHeight_mm(float firstLayerHeight_mm)
    {
        this.firstLayerHeight_mm = firstLayerHeight_mm;
    }

    public float getLayerHeight_mm()
    {
        return layerHeight_mm;
    }

    public void setLayerHeight_mm(float layerHeight_mm)
    {
        this.layerHeight_mm = layerHeight_mm;
    }

    public float getFillDensity_normalised()
    {
        return fillDensity_normalised;
    }

    public void setFillDensity_normalised(float fillDensity_normalised)
    {
        this.fillDensity_normalised = fillDensity_normalised;
    }

    public FillPattern getFillPattern()
    {
        return fillPattern;
    }

    public void setFillPattern(FillPattern fillPattern)
    {
        this.fillPattern = fillPattern;
    }

    public int getFillEveryNLayers()
    {
        return fillEveryNLayers;
    }

    public void setFillEveryNLayers(int fillEveryNLayers)
    {
        this.fillEveryNLayers = fillEveryNLayers;
    }

    public int getSolidLayersAtTop()
    {
        return solidLayersAtTop;
    }

    public void setSolidLayersAtTop(int solidLayersAtTop)
    {
        this.solidLayersAtTop = solidLayersAtTop;
    }

    public int getSolidLayersAtBottom()
    {
        return solidLayersAtBottom;
    }

    public void setSolidLayersAtBottom(int solidLayersAtBottom)
    {
        this.solidLayersAtBottom = solidLayersAtBottom;
    }

    public int getNumberOfPerimeters()
    {
        return numberOfPerimeters;
    }

    public void setNumberOfPerimeters(int numberOfPerimeters)
    {
        this.numberOfPerimeters = numberOfPerimeters;
    }

    public int getBrimWidth_mm()
    {
        return brimWidth_mm;
    }

    public void setBrimWidth_mm(int brimWidth_mm)
    {
        this.brimWidth_mm = brimWidth_mm;
    }

    public boolean getSpiralPrint()
    {
        return spiralPrint;
    }

    public void setSpiralPrint(boolean spiralPrint)
    {
        this.spiralPrint = spiralPrint;
    }

    public float getFirstLayerExtrusionWidth_mm()
    {
        return firstLayerExtrusionWidth_mm;
    }

    public void setFirstLayerExtrusionWidth_mm(float firstLayerExtrusionWidth_mm)
    {
        this.firstLayerExtrusionWidth_mm = firstLayerExtrusionWidth_mm;
    }

    public float getPerimeterExtrusionWidth_mm()
    {
        return perimeterExtrusionWidth_mm;
    }

    public void setPerimeterExtrusionWidth_mm(float perimeterExtrusionWidth_mm)
    {
        this.perimeterExtrusionWidth_mm = perimeterExtrusionWidth_mm;
    }

    public float getFillExtrusionWidth_mm()
    {
        return fillExtrusionWidth_mm;
    }

    public void setFillExtrusionWidth_mm(float fillExtrusionWidth_mm)
    {
        this.fillExtrusionWidth_mm = fillExtrusionWidth_mm;
    }

    public float getSolidFillExtrusionWidth_mm()
    {
        return solidFillExtrusionWidth_mm;
    }

    public void setSolidFillExtrusionWidth_mm(float solidFillExtrusionWidth_mm)
    {
        this.solidFillExtrusionWidth_mm = solidFillExtrusionWidth_mm;
    }

    public float getTopSolidFillExtrusionWidth_mm()
    {
        return topSolidFillExtrusionWidth_mm;
    }

    public void setTopSolidFillExtrusionWidth_mm(float topSolidFillExtrusionWidth_mm)
    {
        this.topSolidFillExtrusionWidth_mm = topSolidFillExtrusionWidth_mm;
    }

    public float getSupportExtrusionWidth_mm()
    {
        return supportExtrusionWidth_mm;
    }

    public void setSupportExtrusionWidth_mm(float supportExtrusionWidth_mm)
    {
        this.supportExtrusionWidth_mm = supportExtrusionWidth_mm;
    }

    public ArrayList<NozzleParameters> getNozzleParameters()
    {
        return nozzleParameters;
    }

    public void setNozzleParameters(ArrayList<NozzleParameters> nozzleParameters)
    {
        this.nozzleParameters = nozzleParameters;
    }

    public int getFirstLayerNozzle()
    {
        return firstLayerNozzle;
    }

    public void setFirstLayerNozzle(int firstLayerNozzle)
    {
        this.firstLayerNozzle = firstLayerNozzle;
    }

    public int getPerimeterNozzle()
    {
        return perimeterNozzle;
    }

    public void setPerimeterNozzle(int perimeterNozzle)
    {
        this.perimeterNozzle = perimeterNozzle;
    }

    public int getFillNozzle()
    {
        return fillNozzle;
    }

    public void setFillNozzle(int fillNozzle)
    {
        this.fillNozzle = fillNozzle;
    }

    public int getSupportNozzle()
    {
        return supportNozzle;
    }

    public void setSupportNozzle(int supportNozzle)
    {
        this.supportNozzle = supportNozzle;
    }

    public int getSupportInterfaceNozzle()
    {
        return supportInterfaceNozzle;
    }

    public void setSupportInterfaceNozzle(int supportInterfaceNozzle)
    {
        this.supportInterfaceNozzle = supportInterfaceNozzle;
    }

    public boolean getGenerateSupportMaterial()
    {
        return generateSupportMaterial;
    }

    public void setGenerateSupportMaterial(boolean generateSupportMaterial)
    {
        this.generateSupportMaterial = generateSupportMaterial;
    }

    public int getSupportOverhangThreshold_degrees()
    {
        return supportOverhangThreshold_degrees;
    }

    public void setSupportOverhangThreshold_degrees(int supportOverhangThreshold_degrees)
    {
        this.supportOverhangThreshold_degrees = supportOverhangThreshold_degrees;
    }

    public int getForcedSupportForFirstNLayers()
    {
        return forcedSupportForFirstNLayers;
    }

    public void setForcedSupportForFirstNLayers(int forcedSupportForFirstNLayers)
    {
        this.forcedSupportForFirstNLayers = forcedSupportForFirstNLayers;
    }

    public SupportPattern getSupportPattern()
    {
        return supportPattern;
    }

    public void setSupportPattern(SupportPattern supportPattern)
    {
        this.supportPattern = supportPattern;
    }

    public float getSupportPatternSpacing_mm()
    {
        return supportPatternSpacing_mm;
    }

    public void setSupportPatternSpacing_mm(float supportPatternSpacing_mm)
    {
        this.supportPatternSpacing_mm = supportPatternSpacing_mm;
    }

    public int getSupportPatternAngle_degrees()
    {
        return supportPatternAngle_degrees;
    }

    public void setSupportPatternAngle_degrees(int supportPatternAngle_degrees)
    {
        this.supportPatternAngle_degrees = supportPatternAngle_degrees;
    }

    public int getPerimeterSpeed_mm_per_s()
    {
        return perimeterSpeed_mm_per_s;
    }

    public void setPerimeterSpeed_mm_per_s(int perimeterSpeed_mm_per_s)
    {
        this.perimeterSpeed_mm_per_s = perimeterSpeed_mm_per_s;
    }

    public int getSmallPerimeterSpeed_mm_per_s()
    {
        return smallPerimeterSpeed_mm_per_s;
    }

    public void setSmallPerimeterSpeed_mm_per_s(int smallPerimeterSpeed_mm_per_s)
    {
        this.smallPerimeterSpeed_mm_per_s = smallPerimeterSpeed_mm_per_s;
    }

    public int getExternalPerimeterSpeed_mm_per_s()
    {
        return externalPerimeterSpeed_mm_per_s;
    }

    public void setExternalPerimeterSpeed_mm_per_s(int externalPerimeterSpeed_mm_per_s)
    {
        this.externalPerimeterSpeed_mm_per_s = externalPerimeterSpeed_mm_per_s;
    }

    public int getFillSpeed_mm_per_s()
    {
        return fillSpeed_mm_per_s;
    }

    public void setFillSpeed_mm_per_s(int fillSpeed_mm_per_s)
    {
        this.fillSpeed_mm_per_s = fillSpeed_mm_per_s;
    }

    public int getSolidFillSpeed_mm_per_s()
    {
        return solidFillSpeed_mm_per_s;
    }

    public void setSolidFillSpeed_mm_per_s(int solidFillSpeed_mm_per_s)
    {
        this.solidFillSpeed_mm_per_s = solidFillSpeed_mm_per_s;
    }

    public int getTopSolidFillSpeed_mm_per_s()
    {
        return topSolidFillSpeed_mm_per_s;
    }

    public void setTopSolidFillSpeed_mm_per_s(int topSolidFillSpeed_mm_per_s)
    {
        this.topSolidFillSpeed_mm_per_s = topSolidFillSpeed_mm_per_s;
    }

    public int getSupportSpeed_mm_per_s()
    {
        return supportSpeed_mm_per_s;
    }

    public void setSupportSpeed_mm_per_s(int supportSpeed_mm_per_s)
    {
        this.supportSpeed_mm_per_s = supportSpeed_mm_per_s;
    }

    public int getBridgeSpeed_mm_per_s()
    {
        return bridgeSpeed_mm_per_s;
    }

    public void setBridgeSpeed_mm_per_s(int bridgeSpeed_mm_per_s)
    {
        this.bridgeSpeed_mm_per_s = bridgeSpeed_mm_per_s;
    }

    public int getGapFillSpeed_mm_per_s()
    {
        return gapFillSpeed_mm_per_s;
    }

    public void setGapFillSpeed_mm_per_s(int gapFillSpeed_mm_per_s)
    {
        this.gapFillSpeed_mm_per_s = gapFillSpeed_mm_per_s;
    }

    public boolean getEnableCooling()
    {
        return enableCooling;
    }

    public void setEnableCooling(boolean enableCooling)
    {
        this.enableCooling = enableCooling;
    }

    public int getMinFanSpeed_percent()
    {
        return minFanSpeed_percent;
    }

    public void setMinFanSpeed_percent(int minFanSpeed_percent)
    {
        this.minFanSpeed_percent = minFanSpeed_percent;
    }

    public int getMaxFanSpeed_percent()
    {
        return maxFanSpeed_percent;
    }

    public void setMaxFanSpeed_percent(int maxFanSpeed_percent)
    {
        this.maxFanSpeed_percent = maxFanSpeed_percent;
    }

    public int getBridgeFanSpeed_percent()
    {
        return bridgeFanSpeed_percent;
    }

    public void setBridgeFanSpeed_percent(int bridgeFanSpeed_percent)
    {
        this.bridgeFanSpeed_percent = bridgeFanSpeed_percent;
    }

    public int getDisableFanFirstNLayers()
    {
        return disableFanFirstNLayers;
    }

    public void setDisableFanFirstNLayers(int disableFanFirstNLayers)
    {
        this.disableFanFirstNLayers = disableFanFirstNLayers;
    }

    public int getCoolIfLayerTimeLessThan_secs()
    {
        return coolIfLayerTimeLessThan_secs;
    }

    public void setCoolIfLayerTimeLessThan_secs(int coolIfLayerTimeLessThan_secs)
    {
        this.coolIfLayerTimeLessThan_secs = coolIfLayerTimeLessThan_secs;
    }

    public int getSlowDownIfLayerTimeLessThan_secs()
    {
        return slowDownIfLayerTimeLessThan_secs;
    }

    public void setSlowDownIfLayerTimeLessThan_secs(int slowDownIfLayerTimeLessThan_secs)
    {
        this.slowDownIfLayerTimeLessThan_secs = slowDownIfLayerTimeLessThan_secs;
    }

    public int getMinPrintSpeed_mm_per_s()
    {
        return minPrintSpeed_mm_per_s;
    }

    public void setMinPrintSpeed_mm_per_s(int minPrintSpeed_mm_per_s)
    {
        this.minPrintSpeed_mm_per_s = minPrintSpeed_mm_per_s;
    }

    @Override
    public SlicerParameters clone()
    {
        SlicerParameters clone = new SlicerParameters();

        clone.profileName = profileName;
        clone.slicerOverride = slicerOverride;

        /*
         * Extrusion data
         */
        clone.firstLayerHeight_mm = firstLayerHeight_mm;
        clone.layerHeight_mm = layerHeight_mm;
        clone.fillDensity_normalised = fillDensity_normalised;
        clone.fillPattern = fillPattern;
        clone.fillEveryNLayers = fillEveryNLayers;
        clone.solidLayersAtTop = solidLayersAtTop;
        clone.solidLayersAtBottom = solidLayersAtBottom;
        clone.numberOfPerimeters = numberOfPerimeters;
        clone.brimWidth_mm = brimWidth_mm;
        clone.spiralPrint = spiralPrint;

        /*
         * Nozzle data
         */
        clone.firstLayerExtrusionWidth_mm = firstLayerExtrusionWidth_mm;
        clone.perimeterExtrusionWidth_mm = perimeterExtrusionWidth_mm;
        clone.fillExtrusionWidth_mm = fillExtrusionWidth_mm;
        clone.solidFillExtrusionWidth_mm = solidFillExtrusionWidth_mm;
        clone.topSolidFillExtrusionWidth_mm = topSolidFillExtrusionWidth_mm;
        clone.supportExtrusionWidth_mm = supportExtrusionWidth_mm;

        clone.nozzleParameters = new ArrayList<>();
        nozzleParameters.stream().forEach(nozzleParameter -> clone.nozzleParameters.add(nozzleParameter.clone()));

        clone.firstLayerNozzle = firstLayerNozzle;
        clone.perimeterNozzle = perimeterNozzle;
        clone.fillNozzle = fillNozzle;
        clone.supportNozzle = supportNozzle;
        clone.supportInterfaceNozzle = supportInterfaceNozzle;

        /*
         * Support
         */
        clone.generateSupportMaterial = generateSupportMaterial;
        clone.supportOverhangThreshold_degrees = supportOverhangThreshold_degrees;
        clone.forcedSupportForFirstNLayers = forcedSupportForFirstNLayers;
        clone.supportPattern = supportPattern;
        clone.supportPatternSpacing_mm = supportPatternSpacing_mm;
        clone.supportPatternAngle_degrees = supportPatternAngle_degrees;

        /*
         * Speed settings
         */
        clone.perimeterSpeed_mm_per_s = perimeterSpeed_mm_per_s;
        clone.smallPerimeterSpeed_mm_per_s = smallPerimeterSpeed_mm_per_s;
        clone.externalPerimeterSpeed_mm_per_s = externalPerimeterSpeed_mm_per_s;
        clone.fillSpeed_mm_per_s = fillSpeed_mm_per_s;
        clone.solidFillSpeed_mm_per_s = solidFillSpeed_mm_per_s;
        clone.topSolidFillSpeed_mm_per_s = topSolidFillSpeed_mm_per_s;
        clone.supportSpeed_mm_per_s = supportSpeed_mm_per_s;
        clone.bridgeSpeed_mm_per_s = bridgeSpeed_mm_per_s;
        clone.gapFillSpeed_mm_per_s = gapFillSpeed_mm_per_s;

        /*
         * Cooling
         */
        clone.enableCooling = enableCooling;
        clone.minFanSpeed_percent = minFanSpeed_percent;
        clone.maxFanSpeed_percent = maxFanSpeed_percent;
        clone.bridgeFanSpeed_percent = bridgeFanSpeed_percent;
        clone.disableFanFirstNLayers = disableFanFirstNLayers;
        clone.coolIfLayerTimeLessThan_secs = coolIfLayerTimeLessThan_secs;
        clone.slowDownIfLayerTimeLessThan_secs = slowDownIfLayerTimeLessThan_secs;
        clone.minPrintSpeed_mm_per_s = minPrintSpeed_mm_per_s;

        return clone;
    }
}
