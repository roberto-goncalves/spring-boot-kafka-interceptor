package com.tracing.firstservice;

import io.opentracing.*;
import io.opentracing.contrib.kafka.HeadersMapExtractAdapter;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import okhttp3.Request;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.*;

import com.google.common.collect.ImmutableMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@RestController
public class FirstServiceController {



    @Autowired
    private Tracer tracer;

    Logger logger = LoggerFactory.getLogger(FirstServiceController.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    Environment environment;


    @RequestMapping(path="/", method= RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public Object acceptPost(@RequestBody Object object) {
        try (Scope scope = TracerSingleton.tracer().buildSpan("receiveObject").startActive(true)){
            scope.span().log(ImmutableMap.of("level", "info", "msg", "receiving object", "object",object));
            logger.debug("Object sent by producer: " + object);
            sendMessage(object.toString());
            scope.span().setTag("level", "info");
            return object;
        }
    }
    @Async
    public void sendMessage(String message) {
        try (Scope scope = TracerSingleton.tracer().buildSpan("sendMessageToKafka").startActive(true)) {
            scope.span().log(ImmutableMap.of("level", "info", "msg", "sending message", "message", message));
            Message<String> msg = MessageBuilder
                    .withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, "jaeger-poc")
                    .setHeader(KafkaHeaders.MESSAGE_KEY, "999")
                    .setHeader(KafkaHeaders.PARTITION_ID, 0)
                    .build();
            ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(msg);
            future.addCallback(new ListenableFutureCallback<SendResult<?, ?>>() {
                @Override
                public void onSuccess(SendResult<?, ?> result) {
                    System.out.println("Sent message=[" + message +
                            "] with offset=[" + result.getRecordMetadata().offset() + "]");
                    scope.span().setTag("level", "info");
                    scope.span().log(ImmutableMap.of("level", "info", "msg", "message sent"));
                }
                @Override
                public void onFailure(Throwable ex) {
                    System.out.println("Unable to send message=["
                            + message + "] due to : " + ex.getMessage());
                    scope.span().setTag("level", "error");
                    scope.span().log(ImmutableMap.of("level", "error", "msg", "failure, message could not be sent " + ex.getMessage()));
                }
            });
        }
    }
}
