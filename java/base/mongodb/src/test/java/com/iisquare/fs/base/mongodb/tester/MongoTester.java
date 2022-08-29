package com.iisquare.fs.base.mongodb.tester;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.mongodb.BasicDBObject;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

public class MongoTester {

    private MongoClient client;

    @Before
    public void init() {
        this.client = MongoClients.create("mongodb://root:admin888@127.0.0.1:27017/");
    }

    @After
    public void destroy() {
        client.close();
    }

    @Test
    public void writeTest() {
        MongoDatabase database = client.getDatabase("fs_project");
        MongoCollection<Document> collection = database.getCollection("fs_test");
        Date date = new Date();
        for (int index = 0; index < 2000000; index++) {
            date.setTime(date.getTime() + DPUtil.random(0, 10));
            String time = DPUtil.millis2dateTime(date.getTime(), "yyyy-MM-dd HH:mm:ss.SSS");
            Document document = new Document();
            document.put("i", index);
            document.put("xn", date.getTime());
            document.put("xd", date);
            document.put("xs", time);
            document.put("nn", date.getTime());
            document.put("nd", date);
            document.put("ns", time);
            collection.insertOne(document);
            if (index % 2000 == 0) {
                System.out.println(String.format("i-%d, n-%d, s-%s", index, date.getTime(), time));
            }
        }
    }

    @Test
    public void columnTest() {
        MongoDatabase database = client.getDatabase("fs_project");
        MongoCollection<Document> collection = database.getCollection("fs_test");
        Bson filter = Filters.in("i", Arrays.asList(1, 2, 3));
        BasicDBObject projection = new BasicDBObject();
        projection.put("xn", 1);
        projection.put("xd", 1);
        projection.put("xs", 1);
        projection.put("xx", 1);
        MongoCursor<Document> cursor = collection.find(filter).projection(projection).cursor();
        while (cursor.hasNext()) {
            Document document = cursor.next();
            System.out.println(document.toJson());
        }
    }

}
