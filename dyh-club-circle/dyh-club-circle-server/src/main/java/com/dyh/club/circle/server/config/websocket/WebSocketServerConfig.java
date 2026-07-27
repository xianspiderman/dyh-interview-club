package com.dyh.club.circle.server.config.websocket;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.dyh.club.circle.server.util.LoginUtil;
import org.springframework.stereotype.Component;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

@Component
public class WebSocketServerConfig extends ServerEndpointConfig.Configurator {

    @Override
    public boolean checkOrigin(String originHeaderValue) {
        return true;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        Map<String, List<String>> parameterMap = request.getParameterMap();
        List<String> erpList = parameterMap.get("erp");
        if(!CollectionUtils.isEmpty(erpList)){
            sec.getUserProperties().put("erp", erpList.get(0));
        }
    }

}
