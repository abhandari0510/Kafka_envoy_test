package com.example.forwarder;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public final class KafkaDetectorForwarder {

    private static final Logger log = LoggerFactory.getLogger(KafkaDetectorForwarder.class);

    private KafkaDetectorForwarder() {
    }

    public static void main(String[] args) {
        String bootstrapServers = env("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String topic = env("KAFKA_TOPIC", "envoy-body-events");
        String groupId = env("KAFKA_DETECTOR_GROUP_ID", "envoy-body-events-detector-forwarder");
        URI detectorUri = URI.create(env("DETECTOR_ENVOY_URL", "http://[::1]:8091/detect"));

        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        HttpClient httpClient = HttpClient.newHttpClient();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(topic));
            log.info("Forwarding Kafka topic '{}' from '{}' to detector Envoy '{}'", topic, bootstrapServers, detectorUri);

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    forwardRecord(httpClient, detectorUri, record);
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        }
    }

    private static void forwardRecord(HttpClient httpClient, URI detectorUri, ConsumerRecord<String, String> record) {
        String body = record.value() == null ? "" : record.value();
        HttpRequest request = HttpRequest.newBuilder(detectorUri)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("Detector Envoy returned HTTP " + statusCode);
            }
            log.info("Forwarded topic={} partition={} offset={} bytes={}",
                    record.topic(), record.partition(), record.offset(), body.length());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send Kafka record to detector Envoy", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending Kafka record to detector Envoy", e);
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
