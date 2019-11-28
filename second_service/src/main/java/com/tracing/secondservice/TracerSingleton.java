package com.tracing.secondservice;

import io.jaegertracing.internal.JaegerTracer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class TracerSingleton {

    private static JaegerTracer tracer = null;

    @Bean
    public static JaegerTracer tracer() {
        if(tracer == null)
            tracer = initTracer("second_service");
        return tracer;
    }

    public static Map processTracerRecords(ConsumerRecords<String, String> records){
        Map<String, String> headersContext = new HashMap<>();
        records.forEach((record) -> {
            Headers headers = record.headers();
            headers.forEach((header) -> {
                headersContext.put(header.key(), new String(header.value()));
            });
        });
        return headersContext;
    }

    public static Map processTracerRecord(ConsumerRecord<String, String> record){
        Map<String, String> headersContext = new HashMap<>();
        record.headers().forEach((header) -> {
                headersContext.put(header.key(), new String(header.value()));
        });
        return headersContext;
    }

    public static JaegerTracer initTracer(String service) {
        io.jaegertracing.Configuration.SamplerConfiguration samplerConfig = io.jaegertracing.Configuration.SamplerConfiguration.fromEnv().withType("const").withParam(1);
        io.jaegertracing.Configuration.ReporterConfiguration reporterConfig = io.jaegertracing.Configuration.ReporterConfiguration.fromEnv().withLogSpans(true);
        io.jaegertracing.Configuration config = new io.jaegertracing.Configuration(service).withSampler(samplerConfig).withReporter(reporterConfig);
        return config.getTracer();
    }
}
