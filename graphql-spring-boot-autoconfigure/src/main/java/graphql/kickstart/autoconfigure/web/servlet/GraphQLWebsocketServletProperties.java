package graphql.kickstart.autoconfigure.web.servlet;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "graphql.servlet.websocket")
public class GraphQLWebsocketServletProperties {
  private boolean enabled = true;

}
