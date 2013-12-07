package com.rekko.etcd;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.ning.http.client.extra.ListenableFutureAdapter.asGuavaFuture;

public class EtcdClient {

  static final AsyncHttpClient CLIENT = new AsyncHttpClient();
  static final Gson gson = new GsonBuilder()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSX")
      .create();


  private final URI _baseUri;
  private final AsyncHttpClient _client;

  public EtcdClient(URI baseUri) {
    this(CLIENT, baseUri);
  }

  public EtcdClient(AsyncHttpClient client, URI baseUri) {
    _client = checkNotNull(client);
    String uri = baseUri.toString();
    if (!uri.endsWith("/")) {
      uri += "/";
      _baseUri = URI.create(uri);
    } else {
      _baseUri = baseUri;

    }
  }

  /**
   * Retrieves a key. Returns null if not found.
   */
  public EtcdResult get(String key) throws EtcdException, IOException {
    URI uri = buildKeyUri("v2/keys", key, "");
    BoundRequestBuilder request = _client.prepareGet(uri.toString());
    EtcdResult result = execute(request);
    return result;
  }


  /**
   * Deletes the given key
   */
  public EtcdResult delete(String key) throws EtcdException, IOException {
    URI uri = buildKeyUri("v2/keys", key, "");
    BoundRequestBuilder delete = _client.prepareDelete(uri.toString());

    return execute(delete);
  }

  /**
   * Sets a key to a new value
   */
  public EtcdResult set(String key, String value) throws EtcdException, IOException {
    return set(key, value, null);
  }

  /**
   * Sets a key to a new value with an (optional) ttl
   */

  public EtcdResult set(String key, String value, Integer ttl)
      throws EtcdException, IOException {
    URI uri = buildKeyUri("v2/keys", key, "");
    BoundRequestBuilder post = _client.preparePut(uri.toString());
    post.addParameter("value", value);
    if (ttl != null) {
      post.addParameter("ttl", Integer.toString(ttl));
    }

    return execute(post);
  }

  /**
   * Sets a key to a new value, if the value is a specified value
   */
  public EtcdResult cas(String key, String prevValue, String value)
      throws EtcdException, IOException {
    return cas(key, prevValue, value, null);

  }

  /**
   * Sets a key to a new value, if the value is a specified value
   */
  public EtcdResult cas(String key, String prevValue, String value, Integer ttl)
      throws EtcdException, IOException {

    URI uri = buildKeyUri("v2/keys", key, "");
    BoundRequestBuilder post = _client.preparePut(uri.toString());
    post.addParameter("value", value);
    post.addParameter("prevValue", prevValue);
    if (ttl != null) {
      post.addParameter("ttl", Integer.toString(ttl));
    }
    return execute(post);
  }

  /**
   * Watches the given subtree
   */
  public ListenableFuture<EtcdResult> watch(String key) throws EtcdException, IOException {
    return watch(key, null);
  }

  /**
   * Watches the given subtree
   */
  public ListenableFuture<EtcdResult> watch(String key, Long index)
      throws EtcdException, IOException {
    URI uri = buildKeyUri("v2/keys", key, "");
    BoundRequestBuilder get = _client.prepareGet(uri.toString());

    get.addQueryParameter("wait", "true");
    get.addQueryParameter("recursive","true");
    if (index != null) {
      get.addQueryParameter("waitIndex", Long.toString(index));

    }

    return asyncExecute(get);
  }

  /**
   * Gets the etcd version
   */
  public String getVersion() throws IOException, EtcdException {
    URI uri = _baseUri.resolve("version");
    BoundRequestBuilder get = _client.prepareGet(uri.toString());
    try {
      Response r = get.execute().get();
      if (r.getStatusCode() != 200) {
        throw new EtcdException(r.getResponseBody(), new EtcdResult(), r.getStatusCode());
      }
      return r.getResponseBody();
    } catch (InterruptedException e) {
      _client.close();
      Thread.currentThread().interrupt();
      return null;
    } catch (ExecutionException e) {
      throw launder(e);
    }
  }


  public List<EtcdResult> listChildren(String key) throws EtcdException, IOException {
    URI uri = buildKeyUri("v2/keys", key, "/");
    BoundRequestBuilder get = _client.prepareGet(uri.toString());
    final EtcdResult result = execute(get);
    return Lists.transform(result.node.nodes, new Function<EtcdResult.Node, EtcdResult>() {
      @Override
      public EtcdResult apply(EtcdResult.Node node) {
        EtcdResult r = new EtcdResult();
        r.action = result.action;
        r.node = node;
        r.cause = result.cause;
        r.errorCode = result.errorCode;
        r.message = result.message;
        return r;
      }
    });
  }

  private EtcdResult jsonToEtcdResult(JsonResponse response, int... expectedErrorCodes) {
    if (response == null || response.json == null) {
      return null;
    }
    EtcdResult result = parseEtcdResult(response.json);
    return result;
  }

  private EtcdResult parseEtcdResult(String json) {
    EtcdResult result = gson.fromJson(json, EtcdResult.class);

    return result;
  }


  private EtcdResult execute(BoundRequestBuilder request) throws IOException, EtcdException {
    try {
      Response response = asGuavaFuture(request.execute()).get();
      JsonResponse json = extractJsonResponse(response);
      EtcdResult result = jsonToEtcdResult(json);
      if (result.isError()) {
        throw new EtcdException(result, response.getStatusCode());
      }
      return result;
    } catch (InterruptedException e) {
      _client.close();
      Thread.currentThread().interrupt();
      return null;
    } catch (ExecutionException e) {
      throw launder(e);
    }
  }

  private ListenableFuture<EtcdResult> asyncExecute(BoundRequestBuilder request) throws IOException,
                                                                                        EtcdException {
    ListenableFuture<Response> response = asGuavaFuture(request.execute());
    return Futures.transform(response, new AsyncFunction<Response, EtcdResult>() {
      @Override
      public ListenableFuture<EtcdResult> apply(Response input) throws Exception {
        JsonResponse json = extractJsonResponse(input);
        return Futures.immediateFuture(jsonToEtcdResult(json));
      }
    });
  }

  private JsonResponse extractJsonResponse(Response response) throws IOException {
    int statusCode = response.getStatusCode();
    String body = response.getResponseBody();

    return new JsonResponse(body, statusCode);
  }

  private URI buildKeyUri(String prefix, String key, String suffix) {
    StringBuilder sb = new StringBuilder();
    sb.append(prefix);
    if (key.startsWith("/")) {
      key = key.substring(1);
    }
    for (String token : Splitter.on('/').split(key)) {
      sb.append("/");
      sb.append(urlEscape(token));
    }
    sb.append(suffix);

    URI uri = _baseUri.resolve(sb.toString());
    return uri;
  }


  protected static String urlEscape(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException();
    }
  }

  public static String format(Object o) {
    try {
      return gson.toJson(o);
    } catch (Exception e) {
      return "Error formatting: " + e.getMessage();
    }
  }

  private RuntimeException launder(ExecutionException e) throws IOException {
    if (e.getCause() instanceof IOException) {
      throw (IOException) e.getCause();
    }
    return Throwables.propagate(e);
  }

  /**
   * We need the status code & the response to parse an error response.
   */
  static class JsonResponse {

    final String json;
    final int httpStatusCode;

    public JsonResponse(String json, int statusCode) {
      this.json = json;
      this.httpStatusCode = statusCode;
    }

  }

}
