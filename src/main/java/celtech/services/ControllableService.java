/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;

/**
 *
 * @author ianhudson
 */
public interface ControllableService
{
    public ReadOnlyStringProperty titleProperty();

    public boolean cancelRun();

    public ReadOnlyBooleanProperty runningProperty();

    public ReadOnlyStringProperty messageProperty();

    public ReadOnlyDoubleProperty progressProperty();
}
