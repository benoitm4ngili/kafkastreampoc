package com.tv2.kafka.poc;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import ott.Input1Value;



public class HP 
{
  
  private static final String IN1TOPIC = "ott.livestatus.test.input-1";

  private static final String BOOTSTRAP_SERVERS = "https://aws-kafka-common-01-tv2-aiven-test.aivencloud.com:26599";
  private static final String SCHEMA_REG_URL    = "https://aws-kafka-common-01-tv2-aiven-test.aivencloud.com:26591";

  // SERVICE USER
  private static String sasl_username = "ott-livestatus-write-user";
  private static String sasl_password = "...";


  static Properties getProperties() {
    Properties props = new Properties();
    
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ott");      
    props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
    props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 5000L);
    props.put("schema.registry.url", SCHEMA_REG_URL);
    props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");        
    props.put("sasl.mechanism", "SCRAM-SHA-256");        
    props.put("sasl.username", sasl_username);        
    props.put("sasl.password", sasl_password);        
    String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
    String jaasConfig = String.format(jaasTemplate, sasl_username, sasl_password);
    props.put("sasl.jaas.config", jaasConfig);
    props.put("ssl.truststore.type",            "jks");
    props.put("ssl.truststore.location",        "client.truststore.jks");
    props.put("ssl.truststore.password",        "...");
    props.put("basic.auth.credentials.source",  "USER_INFO");
    props.put("schema.registry.basic.auth.user.info", sasl_username+":"+sasl_password);
    
    props.put("normalize.schemas",     "true");
    props.put("auto.register.schemas", "true");
    props.put("use.latest.version",    "true");
    
    // SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,   KafkaAvroSerializer.class);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,     StringSerializer.class);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
    props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
    props.put("group.id", "otthp");

    return props;

  }

  public static void main( String[] args )
  {
    Properties prop = getProperties();
    final StreamsBuilder builder = new StreamsBuilder();


    final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", SCHEMA_REG_URL);


    SpecificAvroSerde<Input1Value> inputType1Serde = new SpecificAvroSerde<>();
    // SpecificAvroSerde<Input2Value> inputType2Serde = new SpecificAvroSerde<>();
    // SpecificAvroSerde<OutputValue> outputTypeSerde = new SpecificAvroSerde<>();

    inputType1Serde.configure(serdeConfig, false);
    // inputType2Serde.configure(serdeConfig, false);
    // outputTypeSerde.configure(serdeConfig, false);

    KStream<String, Input1Value> s = builder.stream(IN1TOPIC, Consumed.with(Serdes.String(), inputType1Serde));

    // KTable<String, Input1Value> firstKTable = builder.table(IN1TOPIC, 
    //   Materialized.<String, Input1Value, KeyValueStore<Bytes, byte[]>>as(KTABLE1)
    //     .withKeySerde(Serdes.String())
    //     .withValueSerde(inputType1Serde)
    // );
    // KTable<String, Input2Value> secondKTable = builder.table(IN2TOPIC, 
    //   Materialized.<String, Input2Value, KeyValueStore<Bytes, byte[]>>as(KTABLE2)
    //     .withKeySerde(Serdes.String())
    //     .withValueSerde(inputType2Serde)
    // );

    // KTable<String, OutputValue> table3 = 
    //   firstKTable.join(secondKTable,
    //     (value1, value2) -> new OutputValue(value1.getFullName(), value1.getTeam(), value2.getColour(), value2.getHouse()),
    //     Materialized.<String, OutputValue, KeyValueStore<Bytes, byte[]>>as(KTABLE3)
    //       .withKeySerde(Serdes.String())
    //       .withValueSerde(outputTypeSerde)
    //   );

    // table3    
    //   .toStream()
    //   .peek((key, value) -> System.out.println("output" +key +" value " + value))
    //   .to(OUTTOPIC,
    //     Produced.with(Serdes.String(), outputTypeSerde)
    //   );

    final Topology topology = builder.build();
    // System.out.println(topology.describe());

    try (KafkaStreams kafkaStreams = new KafkaStreams(topology, prop)) {
      final CountDownLatch shutdownLatch = new CountDownLatch(1);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        kafkaStreams.close(Duration.ofSeconds(2));
        shutdownLatch.countDown();
      }));

      try {
        kafkaStreams.start();
        shutdownLatch.await();
      } catch (Throwable e) {
        System.exit(1);
      }
    }

    System.exit(0);        
  }
}
