package com.tv2.kafka.poc;


import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.StreamsConfig;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.Properties;
import org.apache.kafka.common.serialization.StringSerializer;

import ott.Input1Value;

public class ProdAvro1 
{

  private static final String TOPIC             = "ott.livestatus.test.input-1";
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
    props.put("schema.registry.url", SCHEMA_REG_URL);
    props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 5000L);

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

    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
    props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
    props.put("group.id", "ott-prod1");

    return props;

  }
  
  static KafkaProducer<String, Input1Value> getProducer(Properties props) {
    return new KafkaProducer<>(props);
  }

  static Input1Value buildNewInput(Integer i) {
    return Input1Value
            .newBuilder()
            .setFullName("Steve-" + Integer.toString(i))
            .setTeam("idiots")
            .build();
  }

  public static void main( String[] args )
  {

    Properties props = getProperties();

    try (KafkaProducer<String, Input1Value> producer = getProducer(props)) {

      for (int i = 0; i <= 1 ; i++) {
        int j = (int)(Math.random() * 10);
        String key = "key"+Integer.toString(j);
        
        Input1Value input = buildNewInput(1);
        ProducerRecord<String, Input1Value> record = new ProducerRecord<String, Input1Value>(TOPIC, key, input);
    
        producer.send(record,
          (metadata, exception) -> {
            if (exception == null) {
              System.out.println("Message produced "+key+", record metadata: " + metadata);
            } else {
              System.err.println("Error producing message: " + exception.getMessage());
              exception.printStackTrace();
            }
          }
        );
      }
    } catch (Exception e) {
      System.out.println("Could not start producer: " + e.getMessage());
      throw e;
    }
  }
}
