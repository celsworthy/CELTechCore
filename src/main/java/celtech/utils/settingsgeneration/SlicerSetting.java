package celtech.utils.settingsgeneration;

/**
 *
 * @author George
 */
public class SlicerSetting {
    
    private String settingName;
    private String tooltip;
    private String unit;
    
    public SlicerSetting(String settingName) {
        this.settingName = settingName;
    }

    public String getSettingName() {
        return settingName;
    }

    public void setSettingName(String settingName) {
        this.settingName = settingName;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
