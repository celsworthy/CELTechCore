package celtech.utils.settingsgeneration;

import java.util.Optional;

/**
 *
 * @author George
 */
public class SlicerSetting {
    
    private String settingName;
    private String tooltip;
    private Optional<String> unit = Optional.empty();
    
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

    public Optional<String> getUnit() {
        return unit;
    }

    public void setUnit(Optional<String> unit) {
        this.unit = unit;
    }
}
