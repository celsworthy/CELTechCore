package celtech.printerControl.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;

/**
 *
 * @author Ian
 */
public class PrinterIdentity
{

    protected final StringProperty printerUniqueIDProperty = new SimpleStringProperty("");
    protected final StringProperty printermodelProperty = new SimpleStringProperty("");
    protected final StringProperty printereditionProperty = new SimpleStringProperty("");
    protected final StringProperty printerweekOfManufactureProperty = new SimpleStringProperty("");
    protected final StringProperty printeryearOfManufactureProperty = new SimpleStringProperty("");
    protected final StringProperty printerpoNumberProperty = new SimpleStringProperty("");
    protected final StringProperty printerserialNumberProperty = new SimpleStringProperty("");
    protected final StringProperty printercheckByteProperty = new SimpleStringProperty("");
    protected final StringProperty printerFriendlyNameProperty = new SimpleStringProperty("");
    protected final ObjectProperty<Color> printerColourProperty = new SimpleObjectProperty<>();
    protected final StringProperty firmwareVersionProperty = new SimpleStringProperty();

    private final ChangeListener<String> stringChangeListener = new ChangeListener<String>()
    {
        @Override
        public void changed(
            ObservableValue<? extends String> observable, String oldValue, String newValue)
        {
            updatePrinterUniqueID();
        }
    };

    public PrinterIdentity()
    {
        firmwareVersionProperty.addListener(stringChangeListener);

        printerColourProperty.addListener(new ChangeListener<Color>()
        {

            @Override
            public void changed(
                ObservableValue<? extends Color> observable, Color oldValue, Color newValue)
            {
                updatePrinterUniqueID();
            }
        });

        printerFriendlyNameProperty.addListener(stringChangeListener);
        printerUniqueIDProperty.addListener(stringChangeListener);
        printercheckByteProperty.addListener(stringChangeListener);
        printereditionProperty.addListener(stringChangeListener);
        printermodelProperty.addListener(stringChangeListener);
        printerpoNumberProperty.addListener(stringChangeListener);
        printerserialNumberProperty.addListener(stringChangeListener);
        printerweekOfManufactureProperty.addListener(stringChangeListener);
        printeryearOfManufactureProperty.addListener(stringChangeListener);

    }

    public ReadOnlyStringProperty getPrinterUniqueIDProperty()
    {
        return printerUniqueIDProperty;
    }

    /**
     *
     * @return
     */
    public ReadOnlyStringProperty getPrintermodelProperty()
    {
        return printermodelProperty;
    }

    /**
     *
     * @return
     */
    public ReadOnlyStringProperty getPrintereditionProperty()
    {
        return printereditionProperty;
    }

    /**
     *
     * @return
     */
    public ReadOnlyStringProperty getPrinterweekOfManufactureProperty()
    {
        return printerweekOfManufactureProperty;
    }

    /**
     *
     * @return
     */
    public ReadOnlyStringProperty getPrinteryearOfManufactureProperty()
    {
        return printeryearOfManufactureProperty;
    }

    /**
     *
     * @return
     */
    public ReadOnlyStringProperty getPrinterpoNumberProperty()
    {
        return printerpoNumberProperty;
    }

    /**
     *
     * @return
     */
    public ReadOnlyStringProperty getPrinterserialNumberProperty()
    {
        return printerserialNumberProperty;
    }

    /**
     *
     * @return
     */
    public ReadOnlyStringProperty getPrintercheckByteProperty()
    {
        return printercheckByteProperty;
    }

    /**
     *
     * @return
     */
    public ReadOnlyStringProperty getPrinterFriendlyNameProperty()
    {
        return printerFriendlyNameProperty;
    }

    /**
     *
     * @return
     */
    public final ReadOnlyObjectProperty<Color> getPrinterColourProperty()
    {
        return printerColourProperty;
    }

    /**
     *
     * @return
     */
    public final ReadOnlyStringProperty getFirmwareVersionProperty()
    {
        return firmwareVersionProperty;
    }

    /**
     *
     * @return
     */
    private void updatePrinterUniqueID()
    {
        printerUniqueIDProperty.set(printermodelProperty.get()
            + printereditionProperty.get()
            + printerweekOfManufactureProperty.get()
            + printeryearOfManufactureProperty.get()
            + printerpoNumberProperty.get()
            + printerserialNumberProperty.get()
            + printercheckByteProperty.get());
    }

    @Override
    public PrinterIdentity clone()
    {
        PrinterIdentity clone = new PrinterIdentity();
        clone.firmwareVersionProperty.set(firmwareVersionProperty.get());
        clone.printerColourProperty.set(printerColourProperty.get());
        clone.printerFriendlyNameProperty.set(printerFriendlyNameProperty.get());
        clone.printerUniqueIDProperty.set(printerUniqueIDProperty.get());
        clone.printercheckByteProperty.set(printercheckByteProperty.get());
        clone.printereditionProperty.set(printereditionProperty.get());
        clone.printermodelProperty.set(printermodelProperty.get());
        clone.printerpoNumberProperty.set(printerpoNumberProperty.get());
        clone.printerserialNumberProperty.set(printerserialNumberProperty.get());
        clone.printerweekOfManufactureProperty.set(printerweekOfManufactureProperty.get());
        clone.printeryearOfManufactureProperty.set(printeryearOfManufactureProperty.get());

        return clone;
    }
}
