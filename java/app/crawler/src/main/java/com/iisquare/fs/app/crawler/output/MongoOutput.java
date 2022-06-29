package com.iisquare.fs.app.crawler.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MongoOutput extends Output {

    private String database;
    private String collection;
    private String unique;
    private String uri;
    private MongoClient client;
    private MongoCollection<Document> table;

    @Override
    public void configure(JsonNode parameters) {
        String server = parameters.get("server").asText();
        String username = parameters.get("username").asText();
        String password = parameters.get("password").asText();
        database = parameters.get("database").asText();
        collection = parameters.get("collection").asText();
        unique = parameters.get("unique").asText();
        uri = String.format("mongodb://%s:%s@%s/", username, password, server);
    }

    @Override
    public void open() throws IOException {
        client = MongoClients.create(uri);
        MongoDatabase database = client.getDatabase(this.database);
        table = database.getCollection(this.collection);
    }

    @Override
    public void record(JsonNode data) throws Exception {
        List<Document> documents = new ArrayList<>();
        Iterator<JsonNode> iterator = array(data).elements();
        while (iterator.hasNext()) {
            Document document = new Document();
            Iterator<Map.Entry<String, JsonNode>> fields = iterator.next().fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                document.put(entry.getKey(), DPUtil.toJSON(entry.getValue(), Object.class));
            }
            if (document.size() > 0) documents.add(document);
        }
        if (documents.size() < 1) return;
        if (DPUtil.empty(unique)) {
            table.insertMany(documents);
            return;
        }
        UpdateOptions options = new UpdateOptions().upsert(true);
        for (Document document : documents) {
            Bson filter = Filters.eq(unique, document.get(unique));
            document.remove(unique);
            Bson update =  new Document("$set", document);
            table.updateOne(filter, update, options);
        }
    }

    @Override
    public void close() throws IOException {
        FileUtil.close(client);
    }
}
