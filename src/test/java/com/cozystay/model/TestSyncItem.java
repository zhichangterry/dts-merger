package com.cozystay.model;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestSyncItem {
    @Before
    public void setUp() {
        System.out.println("set up " + this.getClass().getName());
    }

    @Test
    public void testEqual() {
        SyncOperation.SyncItem<String> item1 = new SyncOperation.SyncItem<>("abc", "value1", "value2", true);
        SyncOperation.SyncItem<String> item2 = new SyncOperation.SyncItem<>("abc", "value1", "value2", true);
        Assert.assertEquals(item1, item2);
        Assert.assertEquals(item1.hashCode(), item2.hashCode());

        SyncOperation.SyncItem<Integer> item3 = new SyncOperation.SyncItem<>("abc", 123, 456, true);
        SyncOperation.SyncItem<Integer> item4 = new SyncOperation.SyncItem<>("abc", 123, 456, true);
        Assert.assertEquals(item3, item4);
        Assert.assertEquals(item3.hashCode(), item4.hashCode());

    }

    @Test
    public void testEqualNull() {
        SyncOperation.SyncItem<String> item1 = new SyncOperation.SyncItem<>("abc", null, "value2", true);
        SyncOperation.SyncItem<String> item2 = new SyncOperation.SyncItem<>("abc", null, "value2", true);
        Assert.assertEquals(item1, item2);
        Assert.assertEquals(item1.hashCode(), item2.hashCode());
        SyncOperation.SyncItem<String> item3 = new SyncOperation.SyncItem<>("abc", null, null, true);
        SyncOperation.SyncItem<String> item4 = new SyncOperation.SyncItem<>("abc", null, null, true);
        SyncOperation.SyncItem<String> item5 = new SyncOperation.SyncItem<>("abc", "value1", "value2", true);
        Assert.assertEquals(item3, item4);
        Assert.assertEquals(item3.hashCode(), item4.hashCode());
        Assert.assertNotEquals(item2, item5);
    }

    @Test
    public void testSq() {

    }

    @After
    public void tearDown() {
        System.out.println("tear down " + this.getClass().getName());
    }
}
