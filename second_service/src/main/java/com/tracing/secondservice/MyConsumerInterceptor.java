package com.tracing.secondservice;

import com.google.common.collect.ImmutableMap;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class MyConsumerInterceptor implements ConsumerInterceptor<String, String> {


    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
        try (Scope scope = TracerSingleton.tracer()
                .buildSpan("consumerInterceptor")
                .asChildOf(TracerSingleton.tracer()
                        .extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(TracerSingleton.processTracerRecords(records))))
                .startActive(true)){
            scope.span().log(ImmutableMap.of("level", "info", "msg", "received in interceptor"));
        }
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
    }

    @Override
    public void close() {
    }

}
