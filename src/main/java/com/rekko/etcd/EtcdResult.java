package com.rekko.etcd;

import java.util.Date;
import java.util.List;

public class EtcdResult {

  // General values
  String action;
  Node node;

  // For errors
  Integer errorCode;
  String message;
  String cause;


  public boolean isError() {
    return errorCode != null;
  }

  @Override
  public String toString() {
    return EtcdClient.format(this);
  }

  static class Node {

    String key;
    String prevValue;
    String value;
    boolean dir;
    Date expiration;
    int ttl;
    List<Node> nodes;
    long modifiedIndex; //The actual type is uint64
    long createdIndex; //The actual type is unint64
  }

  public String getKey() {
    return node.key;
  }

  public String getPrevValue() {
    return node.prevValue;
  }

  public String getValue() {
    return node.value;
  }

  public boolean isDir() {
    return node.dir;
  }

  public Date getExpiration() {
    return node.expiration;
  }

  public int getTtl() {
    return node.ttl;
  }

  public long getModifiedIndex() {
    return node.modifiedIndex;
  }

  public long getCreatedIndexl() {
    return node.createdIndex;
  }

}
