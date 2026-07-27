package com.dyh.club.wx.handler;

import java.util.Map;
// 定义了接口规范
public interface  WxChatMsgHandler {

    WxChatMsgTypeEnum getMsgType();//告诉我当前的接口是处理什么东西的

    String dealMsg(Map<String, String> messageMap);// 执行各自的业务逻辑

}
