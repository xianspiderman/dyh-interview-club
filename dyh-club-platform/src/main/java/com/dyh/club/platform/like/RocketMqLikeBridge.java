package com.dyh.club.platform.like;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(prefix="club.rocketmq",name="enabled",havingValue="true")
public class RocketMqLikeBridge {
    private final ObjectMapper mapper;private final LikeService likes;
    @Value("${club.rocketmq.name-server}") private String nameServer;
    @Value("${club.rocketmq.topic:subject-liked}") private String topic;
    @Value("${club.rocketmq.producer-group:subject-liked-producer}") private String producerGroup;
    @Value("${club.rocketmq.consumer-group:subject-liked-consumer}") private String consumerGroup;
    @Value("${club.rocketmq.send-retries:2}") private int sendRetries;
    @Value("${club.rocketmq.consume-threads:4}") private int consumeThreads;
    private DefaultMQProducer producer;private DefaultMQPushConsumer consumer;

    public RocketMqLikeBridge(ObjectMapper mapper,@Lazy LikeService likes){this.mapper=mapper;this.likes=likes;}

    @PostConstruct public void start() throws Exception{
        producer=new DefaultMQProducer(producerGroup);producer.setNamesrvAddr(nameServer);producer.setRetryTimesWhenSendFailed(sendRetries);producer.start();
        consumer=new DefaultMQPushConsumer(consumerGroup);consumer.setNamesrvAddr(nameServer);consumer.subscribe(topic,"*");consumer.setConsumeThreadMin(1);consumer.setConsumeThreadMax(consumeThreads);consumer.setMaxReconsumeTimes(5);
        consumer.registerMessageListener((java.util.List<MessageExt> messages,org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext context)->{
            try{for(MessageExt message:messages)likes.consume(mapper.readValue(message.getBody(),LikeEvent.class));return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;}
            catch(Exception error){return ConsumeConcurrentlyStatus.RECONSUME_LATER;}
        });consumer.start();
    }

    public SendResult send(LikeEvent event)throws Exception{
        Message message=new Message(topic,"like-state",event.businessKey(),mapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8));
        return producer.send(message,(queues,msg,arg)->queues.get(Math.floorMod(arg.hashCode(),queues.size())),event.businessKey());
    }

    @PreDestroy public void close(){if(consumer!=null)consumer.shutdown();if(producer!=null)producer.shutdown();}
}
