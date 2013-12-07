package com.rekko.etcd;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

//This test was mostly taken from github.com/justinsb/jetcd they hold the Copyright.
public class SmokeTest {

  String prefix;
  EtcdClient client;

  @Before
  public void initialize() {
    this.prefix = "/unittest-" + UUID.randomUUID().toString();
    this.client = new EtcdClient(URI.create("http://127.0.0.1:4001/"));
  }

  @Test
  public void setAndGet() throws Exception {
    String key = prefix + "/message";

    EtcdResult result;

    result = this.client.set(key, "hello");
    assertEquals("set", result.action);
    assertEquals("hello", result.node.value);
    assertEquals(null, result.node.prevValue);

    result = this.client.get(key);
    assertEquals("get", result.action);
    assertEquals("hello", result.node.value);
    assertEquals(null, result.node.prevValue);

    result = this.client.set(key, "world");
    assertEquals("set", result.action);
    assertEquals("world", result.node.value);
    assertEquals("hello", result.node.prevValue);

    result = this.client.get(key);
    assertEquals("get", result.action);
    assertEquals("world", result.node.value);
    assertEquals(null, result.node.prevValue);
  }

  @Test
  public void getNonExistentKey() throws Exception {
    String key = prefix + "/doesnotexist";

    try {
      this.client.get(key);
      fail("we should throw an etcd exception for key not found");
    } catch (EtcdException e) {
      assertTrue(e.isHttpError(400));
      assertTrue(e.isEtcdError(EtcdErrors.KeyNotFound));
    }
  }

  @Test
  public void testDelete() throws Exception {
    String key = prefix + "/testDelete";

    EtcdResult result;

    result = this.client.set(key, "hello");

    result = this.client.get(key);
    assertEquals("hello", result.node.value);

    result = this.client.delete(key);
    assertEquals("delete", result.action);
    assertEquals(null, result.node.value);
    assertEquals("hello", result.node.prevValue);

    try {
      this.client.get(key);
      fail("we should throw an etcd exception for key not found");
    } catch (EtcdException e) {
      assertTrue(e.isHttpError(400));
      assertTrue(e.isEtcdError(EtcdErrors.KeyNotFound));
    }
  }

  @Test
  public void deleteNonExistentKey() throws Exception {
    String key = prefix + "/doesnotexist";

    try {
      EtcdResult result = this.client.delete(key);
      fail();
    } catch (EtcdException e) {
      assertTrue(e.isEtcdError(EtcdErrors.KeyNotFound));
    }
  }

  @Test
  public void testTtl() throws Exception {
    String key = prefix + "/ttl";

    EtcdResult result;

    result = this.client.set(key, "hello", 2);
    assertNotNull(result.node.expiration);
    assertTrue(result.node.ttl == 2 || result.node.ttl == 1);

    result = this.client.get(key);
    assertEquals("hello", result.node.value);

    Thread.sleep(3000);

    try {
      this.client.get(key);
      fail("we should throw an etcd exception for key not found, the key has expired");
    } catch (EtcdException e) {
      assertTrue(e.isHttpError(400));
      assertTrue(e.isEtcdError(EtcdErrors.KeyNotFound));
    }
  }

  @Test
  public void testCAS() throws Exception {
    String key = prefix + "/cas";

    EtcdResult result;

    result = this.client.set(key, "hello");
    result = this.client.get(key);
    assertEquals("hello", result.node.value);

    try {
      this.client.cas(key, "world", "world");
      fail("We should get back an exception 'Test Failed'");
    } catch (EtcdException e ) {
      assertTrue(e.isEtcdError(EtcdErrors.TestFailed));
    }
    result = this.client.get(key);
    assertEquals("hello", result.node.value);

    result = this.client.cas(key, "hello", "world");
    assertEquals(false, result.isError());
    result = this.client.get(key);
    assertEquals("world", result.node.value);
  }

  @Test
  public void testWatch() throws Exception {
    String key = prefix + "/watch/f";
    EtcdResult r = this.client.set(key, "f1");
    assertEquals("f1", r.node.value);

    ListenableFuture<EtcdResult> watch =  this.client.watch(key, r.node.modifiedIndex + 1);
    try {
      EtcdResult watchResult = watch.get(100, TimeUnit.MILLISECONDS);
      fail("Subtree watch fired unexpectedly: " + watchResult);
    } catch (TimeoutException e) {
      // Expected
    }

    assertFalse(watch.isDone());

    r = this.client.set(key, "f2");
    assertEquals("f2", r.node.value);

    EtcdResult watchResult = watch.get(100, TimeUnit.MILLISECONDS);

    assertNotNull(r);

    {
      assertEquals(key, watchResult.node.key);
      assertEquals("f2", watchResult.node.value);
      assertEquals("set", watchResult.action);
      assertEquals(r.node.modifiedIndex, watchResult.node.modifiedIndex);
    }

  }

  @Test
  public void testWatchPrefix() throws Exception {
    String key = prefix + "/watchPrefix";

    EtcdResult r = this.client.set(key + "/f2", "f2");
    assertEquals("f2", r.node.value);

    ListenableFuture<EtcdResult> watchFuture =  this.client.watch(key, r.node.modifiedIndex + 1);
    try {
      EtcdResult watchResult = watchFuture.get(100, TimeUnit.MILLISECONDS);
      fail("Subtree watch fired unexpectedly: " + watchResult);
    } catch (TimeoutException e) {
      // Expected
    }

    assertFalse(watchFuture.isDone());

    r = this.client.set(key + "/f1", "f1");
    assertEquals("f1", r.node.value);

    EtcdResult watchResult = watchFuture.get(100, TimeUnit.MILLISECONDS);

    assertNotNull(r);

    {
      assertEquals(key + "/f1", watchResult.node.key);
      assertEquals("f1", watchResult.node.value);
      assertEquals("set", watchResult.action);
      assertEquals(r.node.modifiedIndex, watchResult.node.modifiedIndex);
    }
  }

  @Test
  public void testList() throws Exception {
    String key = prefix + "/dir";

    EtcdResult result;

    result = this.client.set(key + "/f1", "f1");
    assertEquals("f1", result.node.value);
    result = this.client.set(key + "/f2", "f2");
    assertEquals("f2", result.node.value);
    result = this.client.set(key + "/f3", "f3");
    assertEquals("f3", result.node.value);
    result = this.client.set(key + "/subdir1/f", "f");
    assertEquals("f", result.node.value);

    List<EtcdResult> listing = this.client.listChildren(key);
    assertEquals(4, listing.size());

    {
      result = listing.get(0);
      assertEquals(key + "/f1", result.node.key);
      assertEquals("f1", result.node.value);
      assertEquals("get", result.action);
      assertEquals(false, result.node.dir);
    }
    {
      result = listing.get(1);
      assertEquals(key + "/f2", result.node.key);
      assertEquals("f2", result.node.value);
      assertEquals("get", result.action);
      assertEquals(false, result.node.dir);
    }
    {
      result = listing.get(2);
      assertEquals(key + "/f3", result.node.key);
      assertEquals("f3", result.node.value);
      assertEquals("get", result.action);
      assertEquals(false, result.node.dir);
    }
    {
      result = listing.get(3);
      assertEquals(key + "/subdir1", result.node.key);
      assertEquals(null, result.node.value);
      assertEquals("get", result.action);
      assertEquals(true, result.node.dir);
    }
  }

  @Test
  public void testGetVersion() throws Exception {
    String version = this.client.getVersion();
    assertTrue(version.startsWith("etcd v0."));
  }

}
