/*
 * Copyright 2014 CEL UK
 */

package sandbox;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tony
 */
public class ObservableSetExampleTest
{
    
    public ObservableSetExampleTest()
    {
    }
    
    @BeforeClass
    public static void setUpClass()
    {
    }
    
    @AfterClass
    public static void tearDownClass()
    {
    }
    
    @Before
    public void setUp()
    {
    }
    
    @After
    public void tearDown()
    {
    }

    @Test
    public void testAddFoo()
    {
        ObservableSetExample observableSetExample = new ObservableSetExample();
        observableSetExample.addFoo(observableSetExample.new Foo());
    }
    
}
