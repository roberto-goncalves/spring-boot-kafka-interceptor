package com.tracing.firstservice;

import com.google.common.collect.ImmutableMap;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapExtractAdapter;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;

@Component
public class MyProducerInterceptor implements ProducerInterceptor<String, String> {

    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        HttpHeaderInjectAdapter adapter = new HttpHeaderInjectAdapter(record.headers());
        try (Scope scope = TracerSingleton.tracer()
                .buildSpan("producerInterceptor")
                .asChildOf(TracerSingleton.tracer().activeSpan().context())
                .startActive(true)){
            scope.span().log(ImmutableMap.of("level", "info", "msg", "received in interceptor"));
        }
        TracerSingleton.tracer().inject(TracerSingleton.tracer().activeSpan().context(), Format.Builtin.HTTP_HEADERS, adapter);
        //record.headers().add("uber-trace-id", TracerSingleton.tracer().activeSpan().context().toString().getBytes());
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    }

    @Override
    public void close() {
    }

    private static class HttpHeaderInjectAdapter implements TextMap {
        private final Headers headers;

        HttpHeaderInjectAdapter(Headers headers) {
            this.headers = headers;
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(String key, String value) {
            headers.add(key, value.getBytes());
        }
    }
}
