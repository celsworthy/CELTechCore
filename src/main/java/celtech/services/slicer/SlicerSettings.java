/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SlicerSettings implements Serializable
{

    private Stenographer LOCAL_steno = StenographerFactory.getStenographer(SlicerSettings.class.getName());
    private String LOCAL_profileName = null;
    private boolean LOCAL_locked = false;

    //Immutable
    protected StringProperty print_center = new SimpleStringProperty("105,75");
    protected IntegerProperty retract_restart_extra_toolchange = new SimpleIntegerProperty(0);
    protected ObservableList<IntegerProperty> bed_size = FXCollections.observableArrayList(new SimpleIntegerProperty(210), new SimpleIntegerProperty(150));
    protected StringProperty duplicate_grid = new SimpleStringProperty("1,1");
    protected FloatProperty z_offset = new SimpleFloatProperty(0.0f);
    protected StringProperty gcode_flavor = new SimpleStringProperty("robox");
    protected BooleanProperty use_relative_e_distances = new SimpleBooleanProperty(true);
    protected BooleanProperty output_nozzle_control = new SimpleBooleanProperty(true);
    protected IntegerProperty vibration_limit = new SimpleIntegerProperty(0);
    protected StringProperty end_gcode = new SimpleStringProperty("");
    protected StringProperty layer_gcode = new SimpleStringProperty("");
    protected StringProperty toolchange_gcode = new SimpleStringProperty("");
    protected IntegerProperty retract_lift = new SimpleIntegerProperty(0);
    protected IntegerProperty retract_restart_extra = new SimpleIntegerProperty(0);
    protected IntegerProperty retract_before_travel = new SimpleIntegerProperty(1);
    protected BooleanProperty retract_layer_change = new SimpleBooleanProperty(false);
    protected BooleanProperty wipe = new SimpleBooleanProperty(false);
    protected ObservableList<FloatProperty> nozzle_diameter = FXCollections.observableArrayList(new SimpleFloatProperty(0.3f), new SimpleFloatProperty(0.8f));

    protected IntegerProperty perimeter_acceleration = new SimpleIntegerProperty(0);
    protected IntegerProperty infill_acceleration = new SimpleIntegerProperty(0);
    protected IntegerProperty bridge_acceleration = new SimpleIntegerProperty(0);
    protected IntegerProperty default_acceleration = new SimpleIntegerProperty(0);

    protected ObservableList<FloatProperty> nozzle_open_angle = FXCollections.observableArrayList(new SimpleFloatProperty(1), new SimpleFloatProperty(1));
    protected ObservableList<FloatProperty> nozzle_partial_open_angle = FXCollections.observableArrayList(new SimpleFloatProperty(0.5f), new SimpleFloatProperty(0.5f));
    protected ObservableList<FloatProperty> nozzle_close_angle = FXCollections.observableArrayList(new SimpleFloatProperty(0), new SimpleFloatProperty(0));
    protected ObservableList<FloatProperty> nozzle_home_angle = FXCollections.observableArrayList(new SimpleFloatProperty(0), new SimpleFloatProperty(0));

    protected BooleanProperty infill_only_where_needed = new SimpleBooleanProperty(true);
    protected IntegerProperty solid_infill_every_layers = new SimpleIntegerProperty(0);
    protected IntegerProperty fill_angle = new SimpleIntegerProperty(45);
    protected IntegerProperty solid_infill_below_area = new SimpleIntegerProperty(70);
    protected BooleanProperty only_retract_when_crossing_perimeters = new SimpleBooleanProperty(false);
    protected BooleanProperty infill_first = new SimpleBooleanProperty(false);

    protected BooleanProperty cooling = new SimpleBooleanProperty(true);
    protected BooleanProperty fan_always_on = new SimpleBooleanProperty(false);
    protected IntegerProperty max_fan_speed = new SimpleIntegerProperty(30);
    protected IntegerProperty min_fan_speed = new SimpleIntegerProperty(100);
    protected IntegerProperty bridge_fan_speed = new SimpleIntegerProperty(100);
    protected IntegerProperty disable_fan_first_layers = new SimpleIntegerProperty(0);
    protected IntegerProperty fan_below_layer_time = new SimpleIntegerProperty(60);
    protected IntegerProperty slowdown_below_layer_time = new SimpleIntegerProperty(15);
    protected IntegerProperty min_print_speed = new SimpleIntegerProperty(15);

    protected BooleanProperty avoid_crossing_perimeters = new SimpleBooleanProperty(false);
    protected IntegerProperty bridge_flow_ratio = new SimpleIntegerProperty(1);
    protected IntegerProperty brim_width = new SimpleIntegerProperty(0);
    protected BooleanProperty complete_objects = new SimpleBooleanProperty(false);
    protected IntegerProperty duplicate = new SimpleIntegerProperty(1);
    protected IntegerProperty duplicate_distance = new SimpleIntegerProperty(6);
    protected BooleanProperty external_perimeters_first = new SimpleBooleanProperty(false);
    protected BooleanProperty extra_perimeters = new SimpleBooleanProperty(true);
    protected IntegerProperty extruder_clearance_height = new SimpleIntegerProperty(20);
    protected IntegerProperty extruder_clearance_radius = new SimpleIntegerProperty(20);
    protected StringProperty extrusion_axis = new SimpleStringProperty("E");
    protected StringProperty first_layer_extrusion_width = new SimpleStringProperty("120%"); // needs to be :0
    protected FloatProperty first_layer_height = new SimpleFloatProperty(0.2f);
    protected IntegerProperty g0 = new SimpleIntegerProperty(0);
    protected IntegerProperty gcode_arcs = new SimpleIntegerProperty(0);
    protected BooleanProperty gcode_comments = new SimpleBooleanProperty(true);
    protected IntegerProperty infill_extruder = new SimpleIntegerProperty(1);
    protected IntegerProperty min_skirt_length = new SimpleIntegerProperty(5);
    protected StringProperty notes = new SimpleStringProperty("");
    protected StringProperty output_filename_format = new SimpleStringProperty("[input_filename_base].gcode");
    protected IntegerProperty perimeter_extruder = new SimpleIntegerProperty(1);
    protected StringProperty post_process = new SimpleStringProperty("");
    protected BooleanProperty randomize_start = new SimpleBooleanProperty(false);
    protected IntegerProperty resolution = new SimpleIntegerProperty(0);
    protected IntegerProperty retract_length_toolchange = new SimpleIntegerProperty(0);
    protected IntegerProperty rotate = new SimpleIntegerProperty(0);
    protected IntegerProperty scale = new SimpleIntegerProperty(1);
    protected IntegerProperty skirt_distance = new SimpleIntegerProperty(6);
    protected IntegerProperty skirt_height = new SimpleIntegerProperty(1);
    protected IntegerProperty skirts = new SimpleIntegerProperty(0);
    protected StringProperty solid_fill_pattern = new SimpleStringProperty("rectilinear");
    protected IntegerProperty threads = new SimpleIntegerProperty(8);
    protected FloatProperty un_retract_ratio = new SimpleFloatProperty(1.2f);
    protected IntegerProperty support_material_interface_layers = new SimpleIntegerProperty(0);
    protected IntegerProperty support_material_interface_spacing = new SimpleIntegerProperty(0);
    protected IntegerProperty raft_layers = new SimpleIntegerProperty(0);
    protected IntegerProperty travel_speed = new SimpleIntegerProperty(400);

    //End immutable
    //Overridden by firmware
    protected ObservableList<StringProperty> nozzle_offset = FXCollections.observableArrayList(new SimpleStringProperty("0x0x0"), new SimpleStringProperty("0x0x0"));

    protected FloatProperty filament_diameter = new SimpleFloatProperty(1.75f);
    protected FloatProperty extrusion_multiplier = new SimpleFloatProperty(0.9f);

    //END of firmware overridden
    //Advanced controls
    protected StringProperty start_gcode = new SimpleStringProperty("");

    protected IntegerProperty perimeter_nozzle = new SimpleIntegerProperty(1); //DONE
    protected IntegerProperty infill_nozzle = new SimpleIntegerProperty(1); //DONE
    protected IntegerProperty support_material_nozzle = new SimpleIntegerProperty(1); //DONE

    protected BooleanProperty auto_unretract = new SimpleBooleanProperty(true); //DONE
    protected IntegerProperty unretract_length = new SimpleIntegerProperty(0); //DONE
    protected IntegerProperty retract_length = new SimpleIntegerProperty(0); //DONE
    protected IntegerProperty retract_speed = new SimpleIntegerProperty(50); //DONE

    protected ObservableList<FloatProperty> nozzle_finish_unretract_by = FXCollections.observableArrayList(new SimpleFloatProperty(0), new SimpleFloatProperty(0)); //DONE
    protected ObservableList<FloatProperty> nozzle_start_retract_by = FXCollections.observableArrayList(new SimpleFloatProperty(0), new SimpleFloatProperty(0)); //DONE
    protected ObservableList<FloatProperty> nozzle_finish_retract_by = FXCollections.observableArrayList(new SimpleFloatProperty(0), new SimpleFloatProperty(0)); //DONE
    protected ObservableList<FloatProperty> nozzle_finish_open_by = FXCollections.observableArrayList(new SimpleFloatProperty(0), new SimpleFloatProperty(0)); //DONE
    protected ObservableList<FloatProperty> nozzle_start_close_by = FXCollections.observableArrayList(new SimpleFloatProperty(0), new SimpleFloatProperty(0)); //DONE
    protected ObservableList<FloatProperty> nozzle_finish_close_by = FXCollections.observableArrayList(new SimpleFloatProperty(0), new SimpleFloatProperty(0)); //DONE

    protected FloatProperty fill_density = new SimpleFloatProperty(0.4f); //DONE
    protected StringProperty fill_pattern = new SimpleStringProperty("rectilinear"); //DONE
    protected IntegerProperty infill_every_layers = new SimpleIntegerProperty(1);
    protected IntegerProperty bottom_solid_layers = new SimpleIntegerProperty(0);
    protected IntegerProperty top_solid_layers = new SimpleIntegerProperty(0);

    protected BooleanProperty support_material = new SimpleBooleanProperty(false); // DONE
    protected IntegerProperty support_material_threshold = new SimpleIntegerProperty(48);
    protected IntegerProperty support_material_enforce_layers = new SimpleIntegerProperty(0);
    protected StringProperty support_material_pattern = new SimpleStringProperty("rectilinear");
    protected FloatProperty support_material_spacing = new SimpleFloatProperty(2.5f);
    protected IntegerProperty support_material_angle = new SimpleIntegerProperty(0);

    protected FloatProperty layer_height = new SimpleFloatProperty(0.2f); //DONE

    protected IntegerProperty perimeter_speed = new SimpleIntegerProperty(25);
    protected IntegerProperty small_perimeter_speed = new SimpleIntegerProperty(20);
    protected IntegerProperty external_perimeter_speed = new SimpleIntegerProperty(20);
    protected IntegerProperty infill_speed = new SimpleIntegerProperty(30);
    protected IntegerProperty solid_infill_speed = new SimpleIntegerProperty(30);
    protected IntegerProperty top_solid_infill_speed = new SimpleIntegerProperty(30);
    protected IntegerProperty support_material_speed = new SimpleIntegerProperty(30);
    protected IntegerProperty bridge_speed = new SimpleIntegerProperty(20);
    protected IntegerProperty gap_fill_speed = new SimpleIntegerProperty(20);
    protected IntegerProperty first_layer_speed = new SimpleIntegerProperty(18);

    protected BooleanProperty spiral_vase = new SimpleBooleanProperty(false);

    protected FloatProperty perimeter_extrusion_width = new SimpleFloatProperty(0.3f);
    protected FloatProperty infill_extrusion_width = new SimpleFloatProperty(0.3f);
    protected FloatProperty solid_infill_extrusion_width = new SimpleFloatProperty(0.3f);
    protected FloatProperty top_infill_extrusion_width = new SimpleFloatProperty(0.3f);

    protected IntegerProperty perimeters = new SimpleIntegerProperty(3);

    public void setFilament_diameter(float value)
    {
        filament_diameter.set(value);
    }

    public float getFilament_diameter()
    {
        return filament_diameter.get();
    }

    public FloatProperty filament_diameterProperty()
    {
        return filament_diameter;
    }

    public void setExtrusion_multiplier(float value)
    {
        extrusion_multiplier.set(value);
    }

    public float getExtrusion_multiplier()
    {
        return extrusion_multiplier.get();
    }

    public FloatProperty extrusion_multiplierProperty()
    {
        return extrusion_multiplier;
    }

    //END Advanced controls
    //Common options
    //END Common options
    //Other stuff
    public StringProperty getPrint_center()
    {
        return print_center;
    }

    public void setPrint_center(StringProperty print_center)
    {
        this.print_center = print_center;
    }

    public IntegerProperty getRetract_restart_extra_toolchange()
    {
        return retract_restart_extra_toolchange;
    }

    public void setRetract_restart_extra_toolchange(IntegerProperty retract_restart_extra_toolchange)
    {
        this.retract_restart_extra_toolchange = retract_restart_extra_toolchange;
    }

    public ObservableList<IntegerProperty> getBed_size()
    {
        return bed_size;
    }

    public void setBed_size(ObservableList<IntegerProperty> bed_size)
    {
        this.bed_size = bed_size;
    }

    public StringProperty getDuplicate_grid()
    {
        return duplicate_grid;
    }

    public void setDuplicate_grid(StringProperty duplicate_grid)
    {
        this.duplicate_grid = duplicate_grid;
    }

    public FloatProperty getZ_offset()
    {
        return z_offset;
    }

    public void setZ_offset(FloatProperty z_offset)
    {
        this.z_offset = z_offset;
    }

    public StringProperty getGcode_flavor()
    {
        return gcode_flavor;
    }

    public void setGcode_flavor(StringProperty gcode_flavor)
    {
        this.gcode_flavor = gcode_flavor;
    }

    public BooleanProperty getUse_relative_e_distances()
    {
        return use_relative_e_distances;
    }

    public void setUse_relative_e_distances(boolean use_relative_e_distances)
    {
        this.use_relative_e_distances.set(use_relative_e_distances);
    }

    public BooleanProperty getOutput_nozzle_control()
    {
        return output_nozzle_control;
    }

    public void setOutput_nozzle_control(boolean output_nozzle_control)
    {
        this.output_nozzle_control.set(output_nozzle_control);
    }

    public IntegerProperty getVibration_limit()
    {
        return vibration_limit;
    }

    public void setVibration_limit(IntegerProperty vibration_limit)
    {
        this.vibration_limit = vibration_limit;
    }

    public StringProperty getStart_gcode()
    {
        return start_gcode;
    }

    public void setStart_gcode(String start_gcode)
    {
        this.start_gcode.set(start_gcode);
    }

    public StringProperty getEnd_gcode()
    {
        return end_gcode;
    }

    public void setEnd_gcode(String end_gcode)
    {
        this.end_gcode.set(end_gcode);
    }

    public StringProperty getLayer_gcode()
    {
        return layer_gcode;
    }

    public void setLayer_gcode(StringProperty layer_gcode)
    {
        this.layer_gcode = layer_gcode;
    }

    public StringProperty getToolchange_gcode()
    {
        return toolchange_gcode;
    }

    public void setToolchange_gcode(StringProperty toolchange_gcode)
    {
        this.toolchange_gcode = toolchange_gcode;
    }

    public IntegerProperty perimeter_nozzleProperty()
    {
        return perimeter_nozzle;
    }

    public void setPerimeter_nozzle(int perimeter_nozzle)
    {
        this.perimeter_nozzle.set(perimeter_nozzle);
    }

    public IntegerProperty infill_nozzleProperty()
    {
        return infill_nozzle;
    }

    public void setInfill_nozzle(int infill_nozzle)
    {
        this.infill_nozzle.set(infill_nozzle);
    }

    public IntegerProperty support_material_nozzleProperty()
    {
        return support_material_nozzle;
    }

    public void setSupport_material_nozzle(int support_material_nozzle)
    {
        this.support_material_nozzle.set(support_material_nozzle);
    }

    public IntegerProperty getRetract_lift()
    {
        return retract_lift;
    }

    public void setRetract_lift(IntegerProperty retract_lift)
    {
        this.retract_lift = retract_lift;
    }

    public IntegerProperty getRetract_restart_extra()
    {
        return retract_restart_extra;
    }

    public void setRetract_restart_extra(IntegerProperty retract_restart_extra)
    {
        this.retract_restart_extra = retract_restart_extra;
    }

    public IntegerProperty getRetract_before_travel()
    {
        return retract_before_travel;
    }

    public void setRetract_before_travel(IntegerProperty retract_before_travel)
    {
        this.retract_before_travel = retract_before_travel;
    }

    public BooleanProperty getRetract_layer_change()
    {
        return retract_layer_change;
    }

    public void setRetract_layer_change(boolean retract_layer_change)
    {
        this.retract_layer_change.set(retract_layer_change);
    }

    public BooleanProperty getWipe()
    {
        return wipe;
    }

    public void setWipe(boolean wipe)
    {
        this.wipe.set(wipe);
    }

    public ObservableList<FloatProperty> getNozzle_diameter()
    {
        return nozzle_diameter;
    }

    public void setNozzle_diameter(ObservableList<FloatProperty> nozzle_diameter)
    {
        this.nozzle_diameter = nozzle_diameter;
    }

    public IntegerProperty getPerimeter_acceleration()
    {
        return perimeter_acceleration;
    }

    public void setPerimeter_acceleration(IntegerProperty perimeter_acceleration)
    {
        this.perimeter_acceleration = perimeter_acceleration;
    }

    public IntegerProperty getInfill_acceleration()
    {
        return infill_acceleration;
    }

    public void setInfill_acceleration(IntegerProperty infill_acceleration)
    {
        this.infill_acceleration = infill_acceleration;
    }

    public IntegerProperty getBridge_acceleration()
    {
        return bridge_acceleration;
    }

    public void setBridge_acceleration(IntegerProperty bridge_acceleration)
    {
        this.bridge_acceleration = bridge_acceleration;
    }

    public IntegerProperty getDefault_acceleration()
    {
        return default_acceleration;
    }

    public void setDefault_acceleration(IntegerProperty default_acceleration)
    {
        this.default_acceleration = default_acceleration;
    }

    public BooleanProperty getAuto_unretract()
    {
        return auto_unretract;
    }

    public void setAuto_unretract(BooleanProperty auto_unretract)
    {
        this.auto_unretract = auto_unretract;
    }

    public BooleanProperty auto_unretractProperty()
    {
        return auto_unretract;
    }

    public void setUnretract_length(int unretract_length)
    {
        this.unretract_length.set(unretract_length);
    }

    public IntegerProperty unretract_lengthProperty()
    {
        return unretract_length;
    }

    public IntegerProperty retract_lengthProperty()
    {
        return retract_length;
    }

    public void setRetract_length(int retract_length)
    {
        this.retract_length.set(retract_length);
    }

    public IntegerProperty retract_speedProperty()
    {
        return retract_speed;
    }

    public void setRetract_speed(int retract_speed)
    {
        this.retract_speed.set(retract_speed);
    }

    public ObservableList<FloatProperty> getNozzle_finish_unretract_by()
    {
        return nozzle_finish_unretract_by;
    }

    public void setNozzle_finish_unretract_by(ObservableList<FloatProperty> nozzle_finish_unretract_by)
    {
        this.nozzle_finish_unretract_by = nozzle_finish_unretract_by;
    }

    public ObservableList<FloatProperty> getNozzle_start_retract_by()
    {
        return nozzle_start_retract_by;
    }

    public void setNozzle_start_retract_by(ObservableList<FloatProperty> nozzle_start_retract_by)
    {
        this.nozzle_start_retract_by = nozzle_start_retract_by;
    }

    public ObservableList<FloatProperty> getNozzle_finish_retract_by()
    {
        return nozzle_finish_retract_by;
    }

    public void setNozzle_finish_retract_by(ObservableList<FloatProperty> nozzle_finish_retract_by)
    {
        this.nozzle_finish_retract_by = nozzle_finish_retract_by;
    }

    public ObservableList<FloatProperty> getNozzle_finish_open_by()
    {
        return nozzle_finish_open_by;
    }

    public void setNozzle_finish_open_by(ObservableList<FloatProperty> nozzle_finish_open_by)
    {
        this.nozzle_finish_open_by = nozzle_finish_open_by;
    }

    public ObservableList<FloatProperty> getNozzle_start_close_by()
    {
        return nozzle_start_close_by;
    }

    public void setNozzle_start_close_by(ObservableList<FloatProperty> nozzle_start_close_by)
    {
        this.nozzle_start_close_by = nozzle_start_close_by;
    }

    public ObservableList<FloatProperty> getNozzle_finish_close_by()
    {
        return nozzle_finish_close_by;
    }

    public void setNozzle_finish_close_by(ObservableList<FloatProperty> nozzle_finish_close_by)
    {
        this.nozzle_finish_close_by = nozzle_finish_close_by;
    }

    public ObservableList<FloatProperty> getNozzle_open_angle()
    {
        return nozzle_open_angle;
    }

    public void setNozzle_open_angle(ObservableList<FloatProperty> nozzle_open_angle)
    {
        this.nozzle_open_angle = nozzle_open_angle;
    }

    public ObservableList<FloatProperty> getNozzle_partial_open_angle()
    {
        return nozzle_partial_open_angle;
    }

    public void setNozzle_partial_open_angle(ObservableList<FloatProperty> nozzle_partial_open_angle)
    {
        this.nozzle_partial_open_angle = nozzle_partial_open_angle;
    }

    public ObservableList<FloatProperty> getNozzle_close_angle()
    {
        return nozzle_close_angle;
    }

    public void setNozzle_close_angle(ObservableList<FloatProperty> nozzle_close_angle)
    {
        this.nozzle_close_angle = nozzle_close_angle;
    }

    public ObservableList<FloatProperty> getNozzle_home_angle()
    {
        return nozzle_home_angle;
    }

    public void setNozzle_home_angle(ObservableList<FloatProperty> nozzle_home_angle)
    {
        this.nozzle_home_angle = nozzle_home_angle;
    }

    public BooleanProperty getFan_always_on()
    {
        return fan_always_on;
    }

    public void setFan_always_on(BooleanProperty fan_always_on)
    {
        this.fan_always_on = fan_always_on;
    }

    public BooleanProperty getCooling()
    {
        return cooling;
    }

    public void setCooling(BooleanProperty cooling)
    {
        this.cooling = cooling;
    }

    public IntegerProperty getMax_fan_speed()
    {
        return max_fan_speed;
    }

    public void setMax_fan_speed(IntegerProperty max_fan_speed)
    {
        this.max_fan_speed = max_fan_speed;
    }

    public IntegerProperty getMin_fan_speed()
    {
        return min_fan_speed;
    }

    public void setMin_fan_speed(IntegerProperty min_fan_speed)
    {
        this.min_fan_speed = min_fan_speed;
    }

    public IntegerProperty getBridge_fan_speed()
    {
        return bridge_fan_speed;
    }

    public void setBridge_fan_speed(IntegerProperty bridge_fan_speed)
    {
        this.bridge_fan_speed = bridge_fan_speed;
    }

    public IntegerProperty getDisable_fan_first_layers()
    {
        return disable_fan_first_layers;
    }

    public void setDisable_fan_first_layers(IntegerProperty disable_fan_first_layers)
    {
        this.disable_fan_first_layers = disable_fan_first_layers;
    }

    public IntegerProperty getFan_below_layer_time()
    {
        return fan_below_layer_time;
    }

    public void setFan_below_layer_time(IntegerProperty fan_below_layer_time)
    {
        this.fan_below_layer_time = fan_below_layer_time;
    }

    public IntegerProperty getSlowdown_below_layer_time()
    {
        return slowdown_below_layer_time;
    }

    public void setSlowdown_below_layer_time(IntegerProperty slowdown_below_layer_time)
    {
        this.slowdown_below_layer_time = slowdown_below_layer_time;
    }

    public IntegerProperty getMin_print_speed()
    {
        return min_print_speed;
    }

    public void setMin_print_speed(IntegerProperty min_print_speed)
    {
        this.min_print_speed = min_print_speed;
    }

    public FloatProperty fill_densityProperty()
    {
        return fill_density;
    }

    public void setFill_density(float fill_density)
    {
        this.fill_density.set(fill_density);
    }

    public StringProperty fill_patternProperty()
    {
        return fill_pattern;
    }

    public void setFill_pattern(String fill_pattern)
    {
        this.fill_pattern.set(fill_pattern);
    }

    public IntegerProperty infill_every_layersProperty()
    {
        return infill_every_layers;
    }

    public void setInfill_every_layers(int infill_every_layers)
    {
        this.infill_every_layers.set(infill_every_layers);
    }

    public BooleanProperty getInfill_only_where_needed()
    {
        return infill_only_where_needed;
    }

    public void setInfill_only_where_needed(boolean infill_only_where_needed)
    {
        this.infill_only_where_needed.set(infill_only_where_needed);
    }

    public IntegerProperty getSolid_infill_every_layers()
    {
        return solid_infill_every_layers;
    }

    public void setSolid_infill_every_layers(int solid_infill_every_layers)
    {
        this.solid_infill_every_layers.set(solid_infill_every_layers);
    }

    public IntegerProperty getFill_angle()
    {
        return fill_angle;
    }

    public void setFill_angle(IntegerProperty fill_angle)
    {
        this.fill_angle = fill_angle;
    }

    public IntegerProperty getSolid_infill_below_area()
    {
        return solid_infill_below_area;
    }

    public void setSolid_infill_below_area(IntegerProperty solid_infill_below_area)
    {
        this.solid_infill_below_area = solid_infill_below_area;
    }

    public BooleanProperty getOnly_retract_when_crossing_perimeters()
    {
        return only_retract_when_crossing_perimeters;
    }

    public BooleanProperty getInfill_first()
    {
        return infill_first;
    }

    public IntegerProperty perimeter_speedProperty()
    {
        return perimeter_speed;
    }

    public void setPerimeter_speed(int perimeter_speed)
    {
        this.perimeter_speed.set(perimeter_speed);
    }

    public IntegerProperty small_perimeter_speedProperty()
    {
        return small_perimeter_speed;
    }

    public void setSmall_perimeter_speed(int small_perimeter_speed)
    {
        this.small_perimeter_speed.set(small_perimeter_speed);
    }

    public IntegerProperty external_perimeter_speedProperty()
    {
        return external_perimeter_speed;
    }

    public void setExternal_perimeter_speed(int external_perimeter_speed)
    {
        this.external_perimeter_speed.set(external_perimeter_speed);
    }

    public IntegerProperty infill_speedProperty()
    {
        return infill_speed;
    }

    public void setInfill_speed(int infill_speed)
    {
        this.infill_speed.set(infill_speed);
    }

    public IntegerProperty solid_infill_speedProperty()
    {
        return solid_infill_speed;
    }

    public void setSolid_infill_speed(int solid_infill_speed)
    {
        this.solid_infill_speed.set(solid_infill_speed);
    }

    public IntegerProperty top_solid_infill_speedProperty()
    {
        return top_solid_infill_speed;
    }

    public void setTop_solid_infill_speed(int top_solid_infill_speed)
    {
        this.top_solid_infill_speed.set(top_solid_infill_speed);
    }

    public IntegerProperty support_material_speedProperty()
    {
        return support_material_speed;
    }

    public void setSupport_material_speed(int support_material_speed)
    {
        this.support_material_speed.set(support_material_speed);
    }

    public IntegerProperty bridge_speedProperty()
    {
        return bridge_speed;
    }

    public void setBridge_speed(int bridge_speed)
    {
        this.bridge_speed.set(bridge_speed);
    }

    public IntegerProperty gap_fill_speedProperty()
    {
        return gap_fill_speed;
    }

    public void setGap_fill_speed(int gap_fill_speed)
    {
        this.gap_fill_speed.set(gap_fill_speed);
    }

    public IntegerProperty travel_speedProperty()
    {
        return travel_speed;
    }

    public void setTravel_speed(int travel_speed)
    {
        this.travel_speed.set(travel_speed);
    }

    public IntegerProperty first_layer_speedProperty()
    {
        return first_layer_speed;
    }

    public void setFirst_layer_speed(int first_layer_speed)
    {
        this.first_layer_speed.set(first_layer_speed);
    }

    public IntegerProperty support_material_thresholdProperty()
    {
        return support_material_threshold;
    }

    public void setSupport_material_threshold(int support_material_threshold)
    {
        this.support_material_threshold.set(support_material_threshold);
    }

    public IntegerProperty support_material_enforce_layersProperty()
    {
        return support_material_enforce_layers;
    }

    public void setSupport_material_enforce_layers(int support_material_enforce_layers)
    {
        this.support_material_enforce_layers.set(support_material_enforce_layers);
    }

    public IntegerProperty getRaft_layers()
    {
        return raft_layers;
    }

    public void setRaft_layers(IntegerProperty raft_layers)
    {
        this.raft_layers = raft_layers;
    }

    public StringProperty support_material_patternProperty()
    {
        return support_material_pattern;
    }

    public void setSupport_material_pattern(String support_material_pattern)
    {
        this.support_material_pattern.set(support_material_pattern);
    }

    public FloatProperty support_material_spacingProperty()
    {
        return support_material_spacing;
    }

    public void setSupport_material_spacing(float support_material_spacing)
    {
        this.support_material_spacing.set(support_material_spacing);
    }

    public IntegerProperty support_material_angleProperty()
    {
        return support_material_angle;
    }

    public void setSupport_material_angle(int support_material_angle)
    {
        this.support_material_angle.set(support_material_angle);
    }

    public IntegerProperty getSupport_material_interface_layers()
    {
        return support_material_interface_layers;
    }

    public void setSupport_material_interface_layers(IntegerProperty support_material_interface_layers)
    {
        this.support_material_interface_layers = support_material_interface_layers;
    }

    public IntegerProperty getSupport_material_interface_spacing()
    {
        return support_material_interface_spacing;
    }

    public void setSupport_material_interface_spacing(IntegerProperty support_material_interface_spacing)
    {
        this.support_material_interface_spacing = support_material_interface_spacing;
    }

    public FloatProperty getLayer_height()
    {
        return layer_height;
    }

    public void setLayer_height(FloatProperty layer_height)
    {
        this.layer_height = layer_height;
    }

    public BooleanProperty support_materialProperty()
    {
        return support_material;
    }

    public void setSupport_material(boolean support_material)
    {
        this.support_material.set(support_material);
    }

    public BooleanProperty getAvoid_crossing_perimeters()
    {
        return avoid_crossing_perimeters;
    }

    public void setAvoid_crossing_perimeters(boolean avoid_crossing_perimeters)
    {
        this.avoid_crossing_perimeters.set(avoid_crossing_perimeters);
    }

    public IntegerProperty bottom_solid_layersProperty()
    {
        return bottom_solid_layers;
    }

    public void setBottom_solid_layers(int bottom_solid_layers)
    {
        this.bottom_solid_layers.set(bottom_solid_layers);
    }

    public IntegerProperty getBridge_flow_ratio()
    {
        return bridge_flow_ratio;
    }

    public void setBridge_flow_ratio(IntegerProperty bridge_flow_ratio)
    {
        this.bridge_flow_ratio = bridge_flow_ratio;
    }

    public IntegerProperty getBrim_width()
    {
        return brim_width;
    }

    public void setBrim_width(IntegerProperty brim_width)
    {
        this.brim_width = brim_width;
    }

    public BooleanProperty getComplete_objects()
    {
        return complete_objects;
    }

    public IntegerProperty getDuplicate()
    {
        return duplicate;
    }

    public void setDuplicate(IntegerProperty duplicate)
    {
        this.duplicate = duplicate;
    }

    public IntegerProperty getDuplicate_distance()
    {
        return duplicate_distance;
    }

    public void setDuplicate_distance(IntegerProperty duplicate_distance)
    {
        this.duplicate_distance = duplicate_distance;
    }

    public BooleanProperty getExternal_perimeters_first()
    {
        return external_perimeters_first;
    }

    public BooleanProperty getExtra_perimeters()
    {
        return extra_perimeters;
    }

    public IntegerProperty getExtruder_clearance_height()
    {
        return extruder_clearance_height;
    }

    public void setExtruder_clearance_height(IntegerProperty extruder_clearance_height)
    {
        this.extruder_clearance_height = extruder_clearance_height;
    }

    public IntegerProperty getExtruder_clearance_radius()
    {
        return extruder_clearance_radius;
    }

    public void setExtruder_clearance_radius(IntegerProperty extruder_clearance_radius)
    {
        this.extruder_clearance_radius = extruder_clearance_radius;
    }

    public StringProperty getExtrusion_axis()
    {
        return extrusion_axis;
    }

    public void setExtrusion_axis(StringProperty extrusion_axis)
    {
        this.extrusion_axis = extrusion_axis;
    }

    public StringProperty getFirst_layer_extrusion_width()
    {
        return first_layer_extrusion_width;
    }

    public void setFirst_layer_extrusion_width(StringProperty first_layer_extrusion_width)
    {
        this.first_layer_extrusion_width = first_layer_extrusion_width;
    }

    public FloatProperty getFirst_layer_height()
    {
        return first_layer_height;
    }

    public void setFirst_layer_height(FloatProperty first_layer_height)
    {
        this.first_layer_height = first_layer_height;
    }

    public IntegerProperty getG0()
    {
        return g0;
    }

    public void setG0(IntegerProperty g0)
    {
        this.g0 = g0;
    }

    public IntegerProperty getGcode_arcs()
    {
        return gcode_arcs;
    }

    public void setGcode_arcs(IntegerProperty gcode_arcs)
    {
        this.gcode_arcs = gcode_arcs;
    }

    public BooleanProperty getGcode_comments()
    {
        return gcode_comments;
    }

    public IntegerProperty getInfill_extruder()
    {
        return infill_extruder;
    }

    public void setInfill_extruder(IntegerProperty infill_extruder)
    {
        this.infill_extruder = infill_extruder;
    }

    public FloatProperty getInfill_extrusion_width()
    {
        return infill_extrusion_width;
    }

    public void setInfill_extrusion_width(float infill_extrusion_width)
    {
        this.infill_extrusion_width.set(infill_extrusion_width);
    }

    public IntegerProperty getMin_skirt_length()
    {
        return min_skirt_length;
    }

    public void setMin_skirt_length(IntegerProperty min_skirt_length)
    {
        this.min_skirt_length = min_skirt_length;
    }

    public StringProperty getNotes()
    {
        return notes;
    }

    public void setNotes(StringProperty notes)
    {
        this.notes = notes;
    }

    public StringProperty getOutput_filename_format()
    {
        return output_filename_format;
    }

    public void setOutput_filename_format(StringProperty output_filename_format)
    {
        this.output_filename_format = output_filename_format;
    }

    public IntegerProperty getPerimeter_extruder()
    {
        return perimeter_extruder;
    }

    public void setPerimeter_extruder(IntegerProperty perimeter_extruder)
    {
        this.perimeter_extruder = perimeter_extruder;
    }

    public FloatProperty getPerimeter_extrusion_width()
    {
        return perimeter_extrusion_width;
    }

    public void setPerimeter_extrusion_width(float perimeter_extrusion_width)
    {
        this.perimeter_extrusion_width.set(perimeter_extrusion_width);
    }

    public IntegerProperty perimetersProperty()
    {
        return perimeters;
    }

    public void setPerimeters(int perimeters)
    {
        this.perimeters.set(perimeters);
    }

    public StringProperty getPost_process()
    {
        return post_process;
    }

    public void setPost_process(StringProperty post_process)
    {
        this.post_process = post_process;
    }

    public BooleanProperty getRandomize_start()
    {
        return randomize_start;
    }

    public IntegerProperty getResolution()
    {
        return resolution;
    }

    public void setResolution(IntegerProperty resolution)
    {
        this.resolution = resolution;
    }

    public IntegerProperty getRetract_length_toolchange()
    {
        return retract_length_toolchange;
    }

    public void setRetract_length_toolchange(IntegerProperty retract_length_toolchange)
    {
        this.retract_length_toolchange = retract_length_toolchange;
    }

    public IntegerProperty getRotate()
    {
        return rotate;
    }

    public void setRotate(IntegerProperty rotate)
    {
        this.rotate = rotate;
    }

    public IntegerProperty getScale()
    {
        return scale;
    }

    public void setScale(IntegerProperty scale)
    {
        this.scale = scale;
    }

    public IntegerProperty getSkirt_distance()
    {
        return skirt_distance;
    }

    public void setSkirt_distance(IntegerProperty skirt_distance)
    {
        this.skirt_distance = skirt_distance;
    }

    public IntegerProperty getSkirt_height()
    {
        return skirt_height;
    }

    public void setSkirt_height(IntegerProperty skirt_height)
    {
        this.skirt_height = skirt_height;
    }

    public IntegerProperty getSkirts()
    {
        return skirts;
    }

    public void setSkirts(IntegerProperty skirts)
    {
        this.skirts = skirts;
    }

    public StringProperty getSolid_fill_pattern()
    {
        return solid_fill_pattern;
    }

    public void setSolid_fill_pattern(StringProperty solid_fill_pattern)
    {
        this.solid_fill_pattern = solid_fill_pattern;
    }

    public FloatProperty getSolid_infill_extrusion_width()
    {
        return solid_infill_extrusion_width;
    }

    public void setSolid_infill_extrusion_width(float solid_infill_extrusion_width)
    {
        this.solid_infill_extrusion_width.set(solid_infill_extrusion_width);
    }

    public BooleanProperty spiral_vaseProperty()
    {
        return spiral_vase;
    }

    public void setSpiral_vase(boolean spiral_vase)
    {
        this.spiral_vase.set(spiral_vase);
    }

    public IntegerProperty getThreads()
    {
        return threads;
    }

    public void setThreads(IntegerProperty threads)
    {
        this.threads = threads;
    }

    public FloatProperty getTop_infill_extrusion_width()
    {
        return top_infill_extrusion_width;
    }

    public void setTop_infill_extrusion_width(float top_infill_extrusion_width)
    {
        this.top_infill_extrusion_width.set(top_infill_extrusion_width);
    }

    public IntegerProperty top_solid_layersProperty()
    {
        return top_solid_layers;
    }

    public void setTop_solid_layers(int top_solid_layers)
    {
        this.top_solid_layers.set(top_solid_layers);
    }

    public FloatProperty getUn_retract_ratio()
    {
        return un_retract_ratio;
    }

    public void setUn_retract_ratio(FloatProperty un_retract_ratio)
    {
        this.un_retract_ratio = un_retract_ratio;
    }

    public void readFromFile(String profileName, boolean locked, String filename)
    {
        LOCAL_profileName = profileName;
        LOCAL_locked = locked;
        File inputFile = new File(filename);
        BufferedReader fileReader = null;

        try
        {
            Method setBooleanProperty = BooleanProperty.class.getDeclaredMethod("setValue", Boolean.class);
            Method setStringProperty = StringProperty.class.getDeclaredMethod("setValue", String.class);
            Method setIntegerProperty = IntegerProperty.class.getDeclaredMethod("setValue", Number.class);
            Method setFloatProperty = FloatProperty.class.getDeclaredMethod("setValue", Number.class);

            Method observableListGet = List.class.getDeclaredMethod("get", int.class);

            fileReader = new BufferedReader(new FileReader(inputFile));

            String lineToProcess = null;

            while ((lineToProcess = fileReader.readLine()) != null)
            {
                String[] lineParts = lineToProcess.trim().split("[ ]*=[ ]*");
                try
                {
                    Field field = this.getClass().getDeclaredField(lineParts[0]);
                    Class<?> fieldClass = field.getType();

                    if (fieldClass.equals(boolean.class))
                    {
                        boolean value = false;

                        if (lineParts.length == 2 && lineParts[1].equalsIgnoreCase("1"))
                        {
                            value = true;
                        }

                        field.setBoolean(this, value);
                    } else if (fieldClass.equals(BooleanProperty.class))
                    {
                        boolean value = false;

                        if (lineParts.length == 2 && lineParts[1].equalsIgnoreCase("1"))
                        {
                            value = true;
                        }
                        setBooleanProperty.invoke(field.get(this), value);
                    } else if (fieldClass.equals(StringProperty.class))
                    {
                        String value = "";
                        if (lineParts.length == 2)
                        {
                            value = lineParts[1];
                        }
                        setStringProperty.invoke(field.get(this), value);
                    } else if (fieldClass.equals(IntegerProperty.class))
                    {
                        int value = 0;
                        if (lineParts.length == 2)
                        {
                            value = Integer.valueOf(lineParts[1]);
                        } else
                        {
                            LOCAL_steno.warning("Field " + lineParts[0] + " is missing a value");
                        }
                        setIntegerProperty.invoke(field.get(this), value);
                    } else if (fieldClass.equals(FloatProperty.class))
                    {
                        float value = 0;
                        if (lineParts.length == 2)
                        {
                            value = Float.valueOf(lineParts[1]);
                        } else
                        {
                            LOCAL_steno.warning("Field " + lineParts[0] + " is missing a value");
                        }
                        setFloatProperty.invoke(field.get(this), value);
                    } else if (fieldClass.equals(ObservableList.class))
                    {
                        Type genericType = field.getGenericType();
                        Class<?> fieldContentClass = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];

                        String[] elements = lineParts[1].split(",");
                        int elementCounter = 0;

                        for (String element : elements)
                        {
                            if (fieldContentClass.equals(IntegerProperty.class))
                            {
                                IntegerProperty property = (IntegerProperty) observableListGet.invoke(field.get(this), elementCounter);
                                property.set(Integer.valueOf(element));
                            } else if (fieldContentClass.equals(FloatProperty.class))
                            {
                                FloatProperty property = (FloatProperty) observableListGet.invoke(field.get(this), elementCounter);
                                property.set(Float.valueOf(element));
                            } else if (fieldContentClass.equals(StringProperty.class))
                            {
                                StringProperty property = (StringProperty) observableListGet.invoke(field.get(this), elementCounter);
                                property.set(element);
                            }

                            elementCounter++;
                        }
                    } else
                    {
                        LOCAL_steno.error("Couldn't process field " + lineParts[0]);
                    }
                } catch (NoSuchFieldException ex)
                {
                    LOCAL_steno.error("Couldn't parse settings for field " + lineParts[0] + " " + ex);
                } catch (IllegalAccessException ex)
                {
                    LOCAL_steno.error("Access exception whilst setting " + lineParts[0] + " " + ex);
                } catch (IllegalArgumentException ex)
                {
                    LOCAL_steno.error("Illegal argument exception whilst setting " + lineParts[0] + " " + ex);
                } catch (SecurityException ex)
                {
                    LOCAL_steno.error("Security exception whilst setting " + lineParts[0] + " " + ex);
                } catch (InvocationTargetException ex)
                {
                    LOCAL_steno.error("Couldn't set up field " + lineParts[0] + " " + ex);
                }
            }

            fileReader.close();
        } catch (IOException ex)
        {
            LOCAL_steno.error("IO Exception when reading settings file " + filename);
        } catch (NoSuchMethodException ex)
        {
            LOCAL_steno.error("Couldn't establish reflection methods when reading settings file " + filename + " " + ex);
        }
    }

    public void renderToFile(String filename)
    {
        File outputFile = new File(filename);
        FileWriter fileWriter = null;

        try
        {
            fileWriter = new FileWriter(outputFile);

            Field[] fields = this.getClass().getDeclaredFields();

            if (fields.length == 0)
            {
                fields = this.getClass().getSuperclass().getDeclaredFields();
            }

            for (Field field : fields)
            {
                try
                {
                    Class<?> fieldClass = field.getType();

                    if (field.getName().startsWith("LOCAL") == false)
                    {

                        if (fieldClass.isArray())
                        {
                            StringBuilder sb = new StringBuilder();
                            sb.append(field.getName());
                            sb.append(" = ");
                            Object fieldValue = field.get(this);

                            int length = Array.getLength(fieldValue);
                            for (int i = 0; i < length; i++)
                            {
                                Object arrayElement = Array.get(fieldValue, i);
                                sb.append(arrayElement);
                                if (i < (length - 1))
                                {
                                    sb.append(",");
                                }
                            }
                            sb.append("\n");
                            fileWriter.write(sb.toString());

                        } else if (fieldClass.equals(boolean.class
                        ))
                        {
                            String name = field.getName();
                            boolean value = field.getBoolean(this);

                            fileWriter.write(name);

                            fileWriter.write(
                                    " = ");

                            if (value
                                    == true)
                            {
                                fileWriter.write("1\n");
                            } else
                            {
                                fileWriter.write("\n");
                            }
                        } else if (fieldClass.equals(StringProperty.class
                        ))
                        {
                            String name = field.getName();
                            StringProperty value = (StringProperty) field.get(this);

                            fileWriter.write(name);

                            fileWriter.write(
                                    " = ");
                            fileWriter.write(value.get());
                            fileWriter.write(
                                    "\n");
                        } else if (fieldClass.equals(IntegerProperty.class
                        ))
                        {
                            String name = field.getName();
                            IntegerProperty value = (IntegerProperty) field.get(this);

                            fileWriter.write(name);

                            fileWriter.write(
                                    " = ");
                            fileWriter.write(value.asString().get());
                            fileWriter.write(
                                    "\n");
                        } else if (fieldClass.equals(FloatProperty.class
                        ))
                        {
                            String name = field.getName();
                            FloatProperty value = (FloatProperty) field.get(this);

                            fileWriter.write(name);

                            fileWriter.write(
                                    " = ");
                            fileWriter.write(String.format("%.2f", value.get()));
                            fileWriter.write(
                                    "\n");
                        } else if (fieldClass.equals(ObservableList.class
                        ))
                        {
                            StringBuilder sb = new StringBuilder();

                            sb.append(field.getName());
                            sb.append(
                                    " = ");
                            ObservableList fieldValue = (ObservableList) field.get(this);
//
                            int length = fieldValue.size();

                            for (int i = 0;
                                    i < length;
                                    i++)
                            {
                                Object arrayElement = fieldValue.get(i);
                                if (arrayElement instanceof IntegerProperty)
                                {
                                    sb.append(((IntegerProperty) arrayElement).get());
                                } else if (arrayElement instanceof FloatProperty)
                                {
                                    sb.append(String.format("%.2f", ((FloatProperty) arrayElement).get()));
                                } else if (arrayElement instanceof StringProperty)
                                {
                                    sb.append(((StringProperty) arrayElement).get());
                                }
                                if (i < (length - 1))
                                {
                                    sb.append(",");
                                }
                            }

                            sb.append(
                                    "\n");
                            fileWriter.write(sb.toString());
//                        FloatProperty value = (FloatProperty) field.get(this);
//
//                        fileWriter.write(name);
//                        fileWriter.write(" = ");
//                        fileWriter.write(String.format("%.2f", value.get()));
//                        fileWriter.write("\n");
                        } else if (fieldClass.equals(BooleanProperty.class
                        ))
                        {
                            String name = field.getName();
                            BooleanProperty value = (BooleanProperty) field.get(this);

                            fileWriter.write(name);

                            fileWriter.write(
                                    " = ");
                            if (value.get()
                                    == true)
                            {
                                fileWriter.write("1");
                            }

                            fileWriter.write(
                                    "\n");
                        } else
                        {
//                field.setAccessible(true);

                            String name = field.getName();
                            Object value = field.get(this);

                            fileWriter.write(name);
                            fileWriter.write(" = ");
                            fileWriter.write(value.toString());
                            fileWriter.write("\n");
                        }
                    }
                } catch (IllegalAccessException ex)
                {
                    LOCAL_steno.error("Error whilst outputting setting " + field.getName());
                }
            }

            fileWriter.close();
        } catch (IOException ex)
        {
            LOCAL_steno.error("Error creating settings file " + ex);
        }
    }

    private void writeObject(ObjectOutputStream out)
            throws IOException
    {
        out.writeFloat(filament_diameter.get());
        out.writeFloat(extrusion_multiplier.get());

        out.writeUTF(print_center.get());
        out.writeInt(retract_restart_extra_toolchange.get());
        for (IntegerProperty sizeProp : bed_size)
        {
            out.writeInt(sizeProp.get());
        }
        out.writeUTF(duplicate_grid.get());
        out.writeFloat(z_offset.get());
        out.writeUTF(gcode_flavor.get());
        out.writeBoolean(use_relative_e_distances.get());
        out.writeBoolean(output_nozzle_control.get());
        out.writeInt(vibration_limit.get());
        out.writeUTF(end_gcode.get());
        out.writeUTF(layer_gcode.get());
        out.writeUTF(toolchange_gcode.get());
        out.writeInt(retract_lift.get());
        out.writeInt(retract_restart_extra.get());
        out.writeInt(retract_before_travel.get());
        out.writeBoolean(retract_layer_change.get());
        out.writeBoolean(wipe.get());
        for (FloatProperty nozzleDiameterProp : nozzle_diameter)
        {
            out.writeFloat(nozzleDiameterProp.get());
        }
        out.writeInt(perimeter_acceleration.get());
        out.writeInt(infill_acceleration.get());
        out.writeInt(bridge_acceleration.get());
        out.writeInt(default_acceleration.get());
        for (FloatProperty nozzleProp : nozzle_open_angle)
        {
            out.writeFloat(nozzleProp.get());
        }
        for (FloatProperty nozzleProp : nozzle_close_angle)
        {
            out.writeFloat(nozzleProp.get());
        }
        for (FloatProperty nozzleProp : nozzle_partial_open_angle)
        {
            out.writeFloat(nozzleProp.get());
        }
        for (FloatProperty nozzleProp : nozzle_close_angle)
        {
            out.writeFloat(nozzleProp.get());
        }
        for (FloatProperty nozzleProp : nozzle_home_angle)
        {
            out.writeFloat(nozzleProp.get());
        }
        out.writeBoolean(infill_only_where_needed.get());
        out.writeInt(solid_infill_every_layers.get());
        out.writeInt(fill_angle.get());
        out.writeInt(solid_infill_below_area.get());
        out.writeBoolean(only_retract_when_crossing_perimeters.get());
        out.writeBoolean(infill_first.get());
        out.writeBoolean(cooling.get());
        out.writeBoolean(fan_always_on.get());
        out.writeInt(max_fan_speed.get());
        out.writeInt(min_fan_speed.get());
        out.writeInt(bridge_fan_speed.get());
        out.writeInt(disable_fan_first_layers.get());
        out.writeInt(fan_below_layer_time.get());
        out.writeInt(slowdown_below_layer_time.get());
        out.writeInt(min_print_speed.get());
        out.writeBoolean(avoid_crossing_perimeters.get());
        out.writeInt(bridge_flow_ratio.get());
        out.writeInt(brim_width.get());
        out.writeBoolean(complete_objects.get());
        out.writeInt(duplicate.get());
        out.writeInt(duplicate_distance.get());
        out.writeBoolean(external_perimeters_first.get());
        out.writeBoolean(extra_perimeters.get());
        out.writeInt(extruder_clearance_height.get());
        out.writeInt(extruder_clearance_radius.get());
        out.writeUTF(extrusion_axis.get());
        out.writeUTF(first_layer_extrusion_width.get());
        out.writeFloat(first_layer_height.get());
        out.writeInt(g0.get());
        out.writeInt(gcode_arcs.get());
        out.writeBoolean(gcode_comments.get());
        out.writeInt(infill_extruder.get());
        out.writeInt(min_skirt_length.get());
        out.writeUTF(notes.get());
        out.writeUTF(output_filename_format.get());
        out.writeInt(perimeter_extruder.get());
        out.writeUTF(post_process.get());
        out.writeBoolean(randomize_start.get());
        out.writeInt(resolution.get());
        out.writeInt(retract_length_toolchange.get());
        out.writeInt(rotate.get());
        out.writeInt(scale.get());
        out.writeInt(skirt_distance.get());
        out.writeInt(skirt_height.get());
        out.writeInt(skirts.get());
        out.writeUTF(solid_fill_pattern.get());
        out.writeInt(threads.get());
        out.writeFloat(un_retract_ratio.get());
        out.writeInt(support_material_interface_layers.get());
        out.writeInt(support_material_interface_spacing.get());
        out.writeInt(raft_layers.get());
        out.writeInt(travel_speed.get());
        for (StringProperty nozzleProp : nozzle_offset)
        {
            out.writeUTF(nozzleProp.get());
        }
        out.writeUTF(start_gcode.get());
        out.writeInt(perimeter_nozzle.get());
        out.writeInt(infill_nozzle.get());
        out.writeInt(support_material_nozzle.get());
        out.writeBoolean(auto_unretract.get());
        out.writeInt(unretract_length.get());
        out.writeInt(retract_length.get());
        out.writeInt(retract_speed.get());
        for (FloatProperty nozzleProp : nozzle_finish_unretract_by)
        {
            out.writeFloat(nozzleProp.get());
        }
        for (FloatProperty nozzleProp : nozzle_start_retract_by)
        {
            out.writeFloat(nozzleProp.get());
        }
        for (FloatProperty nozzleProp : nozzle_finish_retract_by)
        {
            out.writeFloat(nozzleProp.get());
        }
        for (FloatProperty nozzleProp : nozzle_finish_open_by)
        {
            out.writeFloat(nozzleProp.get());
        }
        for (FloatProperty nozzleProp : nozzle_start_close_by)
        {
            out.writeFloat(nozzleProp.get());
        }
        for (FloatProperty nozzleProp : nozzle_finish_close_by)
        {
            out.writeFloat(nozzleProp.get());
        }
        out.writeFloat(fill_density.get());
        out.writeUTF(fill_pattern.get());
        out.writeInt(infill_every_layers.get());
        out.writeInt(bottom_solid_layers.get());
        out.writeInt(top_solid_layers.get());
        out.writeBoolean(support_material.get());
        out.writeInt(support_material_threshold.get());
        out.writeInt(support_material_enforce_layers.get());
        out.writeUTF(support_material_pattern.get());
        out.writeFloat(support_material_spacing.get());
        out.writeInt(support_material_angle.get());
        out.writeFloat(layer_height.get());
        out.writeInt(perimeter_speed.get());
        out.writeInt(small_perimeter_speed.get());
        out.writeInt(external_perimeter_speed.get());
        out.writeInt(infill_speed.get());
        out.writeInt(solid_infill_speed.get());
        out.writeInt(top_solid_infill_speed.get());
        out.writeInt(support_material_speed.get());
        out.writeInt(bridge_speed.get());
        out.writeInt(gap_fill_speed.get());
        out.writeInt(first_layer_speed.get());
        out.writeBoolean(spiral_vase.get());
        out.writeFloat(perimeter_extrusion_width.get());
        out.writeFloat(infill_extrusion_width.get());
        out.writeFloat(solid_infill_extrusion_width.get());
        out.writeFloat(top_infill_extrusion_width.get());
        out.writeInt(perimeters.get());
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        filament_diameter = new SimpleFloatProperty(in.readFloat());
        extrusion_multiplier = new SimpleFloatProperty(in.readFloat());

        print_center = new SimpleStringProperty(in.readUTF());
        retract_restart_extra_toolchange = new SimpleIntegerProperty(in.readInt());
        bed_size = FXCollections.observableArrayList(new SimpleIntegerProperty(in.readInt()), new SimpleIntegerProperty(in.readInt()));
        duplicate_grid = new SimpleStringProperty(in.readUTF());
        z_offset = new SimpleFloatProperty(in.readFloat());
        gcode_flavor = new SimpleStringProperty(in.readUTF());
        use_relative_e_distances = new SimpleBooleanProperty(in.readBoolean());
        output_nozzle_control = new SimpleBooleanProperty(in.readBoolean());
        vibration_limit = new SimpleIntegerProperty(in.readInt());
        end_gcode = new SimpleStringProperty(in.readUTF());
        layer_gcode = new SimpleStringProperty(in.readUTF());
        toolchange_gcode = new SimpleStringProperty(in.readUTF());
        retract_lift = new SimpleIntegerProperty(in.readInt());
        retract_restart_extra = new SimpleIntegerProperty(in.readInt());
        retract_before_travel = new SimpleIntegerProperty(in.readInt());
        retract_layer_change = new SimpleBooleanProperty(in.readBoolean());
        wipe = new SimpleBooleanProperty(in.readBoolean());
        nozzle_diameter = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));
        perimeter_acceleration = new SimpleIntegerProperty(in.readInt());
        infill_acceleration = new SimpleIntegerProperty(in.readInt());
        bridge_acceleration = new SimpleIntegerProperty(in.readInt());
        default_acceleration = new SimpleIntegerProperty(in.readInt());

        nozzle_open_angle = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));
        nozzle_close_angle = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));
        nozzle_partial_open_angle = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));
        nozzle_close_angle = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));
        nozzle_home_angle = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));

        infill_only_where_needed = new SimpleBooleanProperty(in.readBoolean());
        solid_infill_every_layers = new SimpleIntegerProperty(in.readInt());
        fill_angle = new SimpleIntegerProperty(in.readInt());
        solid_infill_below_area = new SimpleIntegerProperty(in.readInt());
        only_retract_when_crossing_perimeters = new SimpleBooleanProperty(in.readBoolean());
        infill_first = new SimpleBooleanProperty(in.readBoolean());
        cooling = new SimpleBooleanProperty(in.readBoolean());
        fan_always_on = new SimpleBooleanProperty(in.readBoolean());
        max_fan_speed = new SimpleIntegerProperty(in.readInt());
        min_fan_speed = new SimpleIntegerProperty(in.readInt());
        bridge_fan_speed = new SimpleIntegerProperty(in.readInt());
        disable_fan_first_layers = new SimpleIntegerProperty(in.readInt());
        fan_below_layer_time = new SimpleIntegerProperty(in.readInt());
        slowdown_below_layer_time = new SimpleIntegerProperty(in.readInt());
        min_print_speed = new SimpleIntegerProperty(in.readInt());
        avoid_crossing_perimeters = new SimpleBooleanProperty(in.readBoolean());
        bridge_flow_ratio = new SimpleIntegerProperty(in.readInt());
        brim_width = new SimpleIntegerProperty(in.readInt());
        complete_objects = new SimpleBooleanProperty(in.readBoolean());
        duplicate = new SimpleIntegerProperty(in.readInt());
        duplicate_distance = new SimpleIntegerProperty(in.readInt());
        external_perimeters_first = new SimpleBooleanProperty(in.readBoolean());
        extra_perimeters = new SimpleBooleanProperty(in.readBoolean());
        extruder_clearance_height = new SimpleIntegerProperty(in.readInt());
        extruder_clearance_radius = new SimpleIntegerProperty(in.readInt());
        extrusion_axis = new SimpleStringProperty(in.readUTF());
        first_layer_extrusion_width = new SimpleStringProperty(in.readUTF());
        first_layer_height = new SimpleFloatProperty(in.readFloat());
        g0 = new SimpleIntegerProperty(in.readInt());
        gcode_arcs = new SimpleIntegerProperty(in.readInt());
        gcode_comments = new SimpleBooleanProperty(in.readBoolean());
        infill_extruder = new SimpleIntegerProperty(in.readInt());
        min_skirt_length = new SimpleIntegerProperty(in.readInt());
        notes = new SimpleStringProperty(in.readUTF());
        output_filename_format = new SimpleStringProperty(in.readUTF());
        perimeter_extruder = new SimpleIntegerProperty(in.readInt());
        post_process = new SimpleStringProperty(in.readUTF());
        randomize_start = new SimpleBooleanProperty(in.readBoolean());
        resolution = new SimpleIntegerProperty(in.readInt());
        retract_length_toolchange = new SimpleIntegerProperty(in.readInt());
        rotate = new SimpleIntegerProperty(in.readInt());
        scale = new SimpleIntegerProperty(in.readInt());
        skirt_distance = new SimpleIntegerProperty(in.readInt());
        skirt_height = new SimpleIntegerProperty(in.readInt());
        skirts = new SimpleIntegerProperty(in.readInt());
        solid_fill_pattern = new SimpleStringProperty(in.readUTF());
        threads = new SimpleIntegerProperty(in.readInt());
        un_retract_ratio = new SimpleFloatProperty(in.readFloat());
        support_material_interface_layers = new SimpleIntegerProperty(in.readInt());
        support_material_interface_spacing = new SimpleIntegerProperty(in.readInt());
        raft_layers = new SimpleIntegerProperty(in.readInt());
        travel_speed = new SimpleIntegerProperty(in.readInt());
        nozzle_offset = FXCollections.observableArrayList(new SimpleStringProperty(in.readUTF()), new SimpleStringProperty(in.readUTF()));
        start_gcode = new SimpleStringProperty(in.readUTF());
        perimeter_nozzle = new SimpleIntegerProperty(in.readInt());
        infill_nozzle = new SimpleIntegerProperty(in.readInt());
        support_material_nozzle = new SimpleIntegerProperty(in.readInt());
        auto_unretract = new SimpleBooleanProperty(in.readBoolean());
        unretract_length = new SimpleIntegerProperty(in.readInt());
        retract_length = new SimpleIntegerProperty(in.readInt());
        retract_speed = new SimpleIntegerProperty(in.readInt());

        nozzle_finish_unretract_by = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));
        nozzle_start_retract_by = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));
        nozzle_finish_retract_by = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));
        nozzle_finish_open_by = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));
        nozzle_start_close_by = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));
        nozzle_finish_close_by = FXCollections.observableArrayList(new SimpleFloatProperty(in.readFloat()), new SimpleFloatProperty(in.readFloat()));

        fill_density = new SimpleFloatProperty(in.readFloat());
        fill_pattern = new SimpleStringProperty(in.readUTF());
        infill_every_layers = new SimpleIntegerProperty(in.readInt());
        bottom_solid_layers = new SimpleIntegerProperty(in.readInt());
        top_solid_layers = new SimpleIntegerProperty(in.readInt());
        support_material = new SimpleBooleanProperty(in.readBoolean());
        support_material_threshold = new SimpleIntegerProperty(in.readInt());
        support_material_enforce_layers = new SimpleIntegerProperty(in.readInt());
        support_material_pattern = new SimpleStringProperty(in.readUTF());
        support_material_spacing = new SimpleFloatProperty(in.readFloat());
        support_material_angle = new SimpleIntegerProperty(in.readInt());
        layer_height = new SimpleFloatProperty(in.readFloat());
        perimeter_speed = new SimpleIntegerProperty(in.readInt());
        small_perimeter_speed = new SimpleIntegerProperty(in.readInt());
        external_perimeter_speed = new SimpleIntegerProperty(in.readInt());
        infill_speed = new SimpleIntegerProperty(in.readInt());
        solid_infill_speed = new SimpleIntegerProperty(in.readInt());
        top_solid_infill_speed = new SimpleIntegerProperty(in.readInt());
        support_material_speed = new SimpleIntegerProperty(in.readInt());
        bridge_speed = new SimpleIntegerProperty(in.readInt());
        gap_fill_speed = new SimpleIntegerProperty(in.readInt());
        first_layer_speed = new SimpleIntegerProperty(in.readInt());
        spiral_vase = new SimpleBooleanProperty(in.readBoolean());
        perimeter_extrusion_width = new SimpleFloatProperty(in.readFloat());
        infill_extrusion_width = new SimpleFloatProperty(in.readFloat());
        solid_infill_extrusion_width = new SimpleFloatProperty(in.readFloat());
        top_infill_extrusion_width = new SimpleFloatProperty(in.readFloat());
        perimeters = new SimpleIntegerProperty(in.readInt());

    }

    private void readObjectNoData()
            throws ObjectStreamException
    {

    }
    
    @Override
    public String toString()
    {
        return LOCAL_profileName;
    }
}
