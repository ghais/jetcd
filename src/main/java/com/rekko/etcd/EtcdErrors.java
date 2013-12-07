/**
 * (C)2013 Digi-Net Technologies, Inc.
 * 5887 Glenridge Drive
 * Suite 350
 * Atlanta, GA 30328 USA
 * All rights reserved.
 */
package com.rekko.etcd;

/**
 */
public class EtcdErrors {

  public static int KeyNotFound = 100;
  public static int TestFailed = 101;
  public static int NotFile = 102;
  public static int NoMorePeer = 103;
  public static int NotDir = 104;
  public static int NodeExist = 105;
  public static int KeyIsPreserved = 106;

  public static int ValueRequired = 200;
  public static int PrevValueRequired = 201;
  public static int TTLNaN = 202;
  public static int IndexNaN = 203;

  public static int RaftInternal = 300;
  public static int LeaderElect = 301;

  public static int WatcherCleared = 400;
  public static int EventIndexCleared = 401;

}
