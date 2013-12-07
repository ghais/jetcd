package com.rekko.etcd;


import static com.google.common.base.Preconditions.checkNotNull;

public class EtcdException extends Exception {

  final int _httpStatusCode;

  final EtcdResult _result;

  public EtcdException(String message, EtcdResult result, int httpStatus) {
    super(message);
    _httpStatusCode = httpStatus;
    _result = checkNotNull(result);
  }

  public EtcdException(EtcdResult result, int httpStatus) {
    this(result.message, result, httpStatus);
  }

  public boolean isHttpError(int httpStatusCode) {
    return (httpStatusCode == _httpStatusCode);
  }

  public boolean isEtcdError(int etcdCode) {
    return (_result != null && _result.errorCode != null
            && etcdCode == _result.errorCode);

  }
}
