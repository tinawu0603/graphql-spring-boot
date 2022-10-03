package graphql.kickstart.autoconfigure.web.servlet;

import graphql.kickstart.autoconfigure.tools.GraphQLJavaToolsAutoConfiguration;
import graphql.kickstart.autoconfigure.web.OnSchemaOrSchemaProviderBean;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionInvocationInputFactory;
import graphql.kickstart.execution.subscriptions.SubscriptionConnectionListener;
import graphql.kickstart.execution.subscriptions.apollo.KeepAliveSubscriptionConnectionListener;
import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.websocket.server.ServerContainer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({DispatcherServlet.class, ServerEndpointRegistration.class})
@Conditional(OnSchemaOrSchemaProviderBean.class)
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@ConditionalOnProperty(
    value = "graphql.servlet.websocket.enabled",
    havingValue = "true",
    matchIfMissing = true)
@AutoConfigureAfter({GraphQLJavaToolsAutoConfiguration.class, GraphQLWebAutoConfiguration.class})
@EnableConfigurationProperties({
    GraphQLWebsocketServletProperties.class,
  GraphQLSubscriptionApolloProperties.class,
  GraphQLSubscriptionWebsocketProperties.class
})
public class GraphQLWebsocketAutoConfiguration {

  private final GraphQLSubscriptionApolloProperties apolloProperties;
  private final GraphQLSubscriptionWebsocketProperties websocketProperties;

  @Bean
  @ConditionalOnMissingBean
  public GraphQLWebsocketServlet graphQLWebsocketServlet(
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker,
      GraphQLObjectMapper graphQLObjectMapper,
      @Autowired(required = false) Collection<SubscriptionConnectionListener> connectionListeners) {
    System.out.println("GraphQL Websocket Auto Configuration GraphQL websocket servlet.");
    Set<SubscriptionConnectionListener> listeners = new HashSet<>();
    if (connectionListeners != null) {
      listeners.addAll(connectionListeners);
    }
    keepAliveListener().ifPresent(listeners::add);
    return new GraphQLWebsocketServlet(
        graphQLInvoker, invocationInputFactory, graphQLObjectMapper, listeners);
  }

  private Optional<SubscriptionConnectionListener> keepAliveListener() {
    if (apolloProperties.isKeepAliveEnabled()) {
      return Optional.of(
          new KeepAliveSubscriptionConnectionListener(
              Duration.ofSeconds(apolloProperties.getKeepAliveIntervalSeconds())));
    }
    return Optional.empty();
  }

  @Bean
  @ConditionalOnClass(ServerContainer.class)
  public ServerEndpointRegistration serverEndpointRegistration(GraphQLWebsocketServlet servlet) {
    System.out.println("GraphQL Websocket Auto Configuration GraphQL server endpoint registration websocket servlet.");
    return new GraphQLWsServerEndpointRegistration(websocketProperties.getPath(), servlet);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(ServerContainer.class)
  public ServerEndpointExporter serverEndpointExporter() {
    return new ServerEndpointExporter();
  }
}
