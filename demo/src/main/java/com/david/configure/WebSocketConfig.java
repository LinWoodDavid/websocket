package com.david.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * =================================
 * Created by David on 2018/11/15.
 * mail:    17610897521@163.com
 * 描述:      webSocket 配置
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter endpointExporter() {
        return new ServerEndpointExporter();
    }

}
