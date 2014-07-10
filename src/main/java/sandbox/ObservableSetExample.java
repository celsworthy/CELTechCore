/*
 * Copyright 2014 CEL UK
 */

package sandbox;

import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 *
 * @author tony
 */
public class ObservableSetExample
{
    
    class Foo {}
    
    private ObservableSet<Foo> foos = FXCollections.observableSet();
    
    public void addFoo(Foo foo) {
        foos.add(foo);
    }
    
}
