package com.tracing.secondservice;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.ImmutableMap;
import io.opentracing.*;
import io.opentracing.contrib.kafka.TracingKafkaConsumer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Service
public class SecondServiceController {

    Logger logger = LoggerFactory.getLogger(SecondServiceController.class);

    @Autowired
    private MessageRepository repo;

    @KafkaListener(topics = "jaeger-poc", groupId = "listener-group")
    public void listen(String message, ConsumerRecord<String, String> consumerRecord) {
        try (Scope scope = TracerSingleton.tracer()
                .buildSpan("receivedFromKafka")
                .asChildOf(TracerSingleton.tracer()
                        .extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(TracerSingleton.processTracerRecord(consumerRecord))))
                .startActive(true)) {
            scope.span().log(ImmutableMap.of("level", "info", "msg", "received from kafka", "message", message));
            logger.debug("Object received by consumer: " + message);
            insertCassandra(message);
        }
    }

    private void insertCassandra(String message){
        try (Scope scope = TracerSingleton.tracer().buildSpan("insertingCassandra").startActive(true)) {
            UUID uuid = UUIDs.timeBased();
            scope.span().log(ImmutableMap.of("level", "info", "msg", "attempt to insert in cassandra", "message", message));
            try{
                repo.save(new Message(uuid, message));
                scope.span().log(ImmutableMap.of("level", "info", "msg", "insert successful, find registry in cassandra: " + repo.findById(uuid).isPresent(), "message", message));
                logger.debug("Getting confirmation on Cassandra..." + repo.findById(uuid).isPresent());
            }catch (Exception ex){
                scope.span().log(ImmutableMap.of("event", "insert failed", "Exception", ex.getMessage()));
                scope.span().setTag("level", "error");
                scope.span().setTag("error", "true");
            }
        }
    }
}
