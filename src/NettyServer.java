import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;


public class NettyServer {
	private class HttpOkResponseHandler extends SimpleChannelUpstreamHandler {

		/* (non-Javadoc)
		 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
		 */
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			HttpRequest request = (HttpRequest) e.getMessage();
			QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
			boolean keepAlive = false;
			String connection = request.getHeader(HttpHeaders.Names.CONNECTION);
			if (connection != null) {
				keepAlive = "keep-alive".equals(connection.toLowerCase());
			}
			StringBuilder buf = new StringBuilder("Hello: ");
			boolean isFirst = true;
			for (Entry<String, List<String>> param : decoder.getParameters().entrySet()) {
				if (isFirst) {
					isFirst = false;
				}
				else {
					buf.append(",");
				}
				buf.append(param.getKey()).append("=").append(param.getValue().get(0));
			}
			
			DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
			response.setContent(ChannelBuffers.copiedBuffer(buf.toString().getBytes(CharsetUtil.UTF_8)));
			
			response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
			response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, response.getContent().readableBytes());
			ChannelFuture future = ctx.getChannel().write(response);
			 if (!keepAlive) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
		}
	}
	
	void run() {
		ChannelFactory factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		ServerBootstrap serverBootstrap = new ServerBootstrap(factory);
		serverBootstrap.setPipelineFactory(
				new ChannelPipelineFactory() {
					@Override
					public ChannelPipeline getPipeline() throws Exception {
						ChannelPipeline pipeline = new DefaultChannelPipeline();
						pipeline.addLast("decorder", new HttpRequestDecoder());
						pipeline.addLast("encoder", new HttpResponseEncoder());
						pipeline.addLast("handler", new HttpOkResponseHandler());
						return pipeline;
					}
				});
		serverBootstrap.setOption("reuseAddress", true);
		serverBootstrap.bind(new InetSocketAddress(8080));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new NettyServer().run();
	}
}
