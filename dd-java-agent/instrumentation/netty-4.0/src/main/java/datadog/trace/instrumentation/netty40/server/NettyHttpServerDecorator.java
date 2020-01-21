// Modified by SignalFx
package datadog.trace.instrumentation.netty40.server;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;

import datadog.trace.agent.decorator.HttpServerDecorator;
import datadog.trace.api.Config;
import datadog.trace.instrumentation.api.AgentSpan;
import datadog.trace.instrumentation.netty40.NettyUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyHttpServerDecorator
    extends HttpServerDecorator<HttpRequest, Channel, HttpResponse> {
  public static final NettyHttpServerDecorator DECORATE = new NettyHttpServerDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"netty", "netty-4.0"};
  }

  @Override
  protected String component() {
    return "netty";
  }

  @Override
  protected String method(final HttpRequest httpRequest) {
    return httpRequest.getMethod().name();
  }

  @Override
  protected URI url(final HttpRequest request) throws URISyntaxException {
    final URI uri = new URI(request.getUri());
    if ((uri.getHost() == null || uri.getHost().equals("")) && request.headers().contains(HOST)) {
      return new URI("http://" + request.headers().get(HOST) + request.getUri());
    } else {
      return uri;
    }
  }

  @Override
  protected String peerHostname(final Channel channel) {
    final SocketAddress socketAddress = channel.remoteAddress();
    if (socketAddress instanceof InetSocketAddress) {
      return ((InetSocketAddress) socketAddress).getHostName();
    }
    return null;
  }

  @Override
  protected String peerHostIP(final Channel channel) {
    final SocketAddress socketAddress = channel.remoteAddress();
    if (socketAddress instanceof InetSocketAddress) {
      return ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
    }
    return null;
  }

  @Override
  protected Integer peerPort(final Channel channel) {
    final SocketAddress socketAddress = channel.remoteAddress();
    if (socketAddress instanceof InetSocketAddress) {
      return ((InetSocketAddress) socketAddress).getPort();
    }
    return null;
  }

  @Override
  protected Integer status(final HttpResponse httpResponse) {
    return httpResponse.getStatus().code();
  }

  @Override
  public AgentSpan onResponse(final AgentSpan span, final HttpResponse response) {
    assert span != null;
    if (response != null) {
      final Integer status = status(response);
      if (status != null) {
        final boolean rewritten = NettyUtils.setServerSpanHttpStatus(span, status);
        if (!rewritten && Config.get().getHttpServerErrorStatuses().contains(status)) {
          span.setError(true);
        }
      }
    }
    return span;
  }
}
