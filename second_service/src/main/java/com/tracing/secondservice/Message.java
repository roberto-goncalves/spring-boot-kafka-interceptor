package com.tracing.secondservice;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table
public class Message {
    @PrimaryKey
    private UUID id;
    private String message;

    public Message(UUID id, String message) {
        this.id = id;
        this.message = message;
    }
}
