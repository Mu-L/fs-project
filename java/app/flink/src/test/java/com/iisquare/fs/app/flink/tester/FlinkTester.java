package com.iisquare.fs.app.flink.tester;

import com.iisquare.fs.app.flink.output.EmptyOutput;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class FlinkTester {

    @Test
    public void batchTest() throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        DataSource<Integer> source = env.fromCollection(data);
        source.print();
        source.output(new EmptyOutput<>());
        env.execute("batch-test");
    }

    @Test
    public void streamTest() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "kafka:9092");
        properties.setProperty("zookeeper.connect", "zookeeper:2181/kafka");
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("auto.commit.enable", "true");
//        properties.setProperty("auto.commit.interval.ms", "1000");
        properties.setProperty("group.id", "fs-test");
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> stream = env.addSource(
                new FlinkKafkaConsumer<>("fs-access-log", new SimpleStringSchema(), properties), getClass().getSimpleName());
        stream.print();
        env.execute("stream-test");
    }

    @Test
    public void sqlTest() {}

    @Test
    public void restTest() throws Exception {
        Configuration config = new Configuration();
        config.setString(RestOptions.BIND_PORT,"8081");
        config.setInteger("taskmanager.numberOfTaskSlots", 3);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        // nc -l 8888
        DataStreamSource<String> source = env.socketTextStream("127.0.0.1", 8888);
        source.print().setParallelism(1);
        env.execute("rest-test");
    }

}
