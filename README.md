jetcd: Java binding for etcd v2 api
===================================
A simple Java client library for the awesome [etcd] (https://github.com/coreos/etcd) V2 api.

Requirements
-----------------
Java 7: To support RFC 3339 timestamps which are used by Go/Etcd
Go 1.2: The timestamp json representation in Go 1.2 have nano second precession in G 1.1 it was
mill second precision. The current parsing assumes nano second precision.

I plan to address those 2 issues soon.

Usage
-----

```Java
EtcdClient client = new EtcdClient(URI.create("http://127.0.0.1:4001/"));

String key = "/watch";

EtcdResult result = this.client.set(key, "hello");
Assert.assertEquals("hello", result.value);

result = this.client.get(key);
Assert.assertEquals("hello", result.value);
        
ListenableFuture<EtcdResult> watchFuture = this.client.watch(key, result.index + 1);
Assert.assertFalse(watchFuture.isDone());

result = this.client.set(key, "world");
Assert.assertEquals("world", result.value);

EtcdResult watchResult = watchFuture.get(100, TimeUnit.MILLISECONDS);
Assert.assertNotNull(result);
Assert.assertEquals("world", result.value);
```


Relationship to [justinsb/jetcd] (https://github.com/justinsb/jetcd)
--------------------------------------------------
The project was initially based on studying [justinsb/jetcd] (https://github.com/justinsb/jetcd)
and re-writing the code to be more compatible with our internal code base at Digi-Net


Many thanks to justinsb for the initial release.

