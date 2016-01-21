package celtech.printerControl.comms.commands.rx;

import celtech.comms.remote.RxPacketTypeEnum;
import celtech.comms.remote.RoboxRxPacket;
import celtech.configuration.MaterialType;
import celtech.printerControl.model.Reel;
import javafx.scene.paint.Color;

/**
 *
 * @author ianhudson
 */
public abstract class ReelEEPROMDataResponse extends RoboxRxPacket
{

    public ReelEEPROMDataResponse(RxPacketTypeEnum packetType, boolean includeSequenceNumber, boolean includeCharsOfDataInOutput)
    {
        super(packetType, includeSequenceNumber, includeCharsOfDataInOutput);
    }

    public abstract void updateContents(Reel attachedReel);

    public abstract void setReelNumber(int reelNumber);

    public abstract String getReelFilamentID();

    public abstract String getReelFriendlyName();

    public abstract int getFirstLayerNozzleTemperature();

    public abstract int getNozzleTemperature();

    public abstract int getFirstLayerBedTemperature();

    public abstract int getBedTemperature();

    public abstract int getAmbientTemperature();

    public abstract float getFilamentDiameter();

    public abstract float getFilamentMultiplier();

    public abstract float getFeedRateMultiplier();

    public abstract float getReelRemainingFilament();

    public abstract MaterialType getReelMaterialType();

    public abstract Color getReelDisplayColour();

    public abstract int getReelNumber();

    public abstract void setReelFilamentID(String reelFilamentID);

    public abstract void setReelFirstLayerNozzleTemperature(int reelFirstLayerNozzleTemperature);

    public abstract void setReelNozzleTemperature(int reelNozzleTemperature);

    public abstract void setReelFirstLayerBedTemperature(int reelFirstLayerBedTemperature);

    public abstract void setReelBedTemperature(int reelBedTemperature);

    public abstract void setReelAmbientTemperature(int reelAmbientTemperature);

    public abstract void setReelFilamentDiameter(float reelFilamentDiameter);

    public abstract void setReelFilamentMultiplier(float reelFilamentMultiplier);

    public abstract void setReelFeedRateMultiplier(float reelFeedRateMultiplier);

    public abstract void setReelRemainingFilament(float reelRemainingFilament);

    public abstract void setReelMaterialType(MaterialType reelMaterialType);

    public abstract void setReelDisplayColour(Color reelDisplayColour);

    public abstract void setReelFriendlyName(String reelFriendlyName);
}
