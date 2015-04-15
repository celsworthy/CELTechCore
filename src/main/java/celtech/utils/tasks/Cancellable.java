/*
 * Copyright 2014 CEL UK
 */
package celtech.utils.tasks;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author tony
 */
public class Cancellable
{
    public BooleanProperty cancelled = new SimpleBooleanProperty(false);
}
