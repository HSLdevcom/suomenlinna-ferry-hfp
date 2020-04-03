package fi.hsl.suomenlinna_hfp.health;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;

import java.io.*;

public class HealthNotificationService {


    private final String postEndpoint;
    private final CloseableHttpClient apacheDefaultClient;

    public HealthNotificationService(String postEndpoint) {
        this.postEndpoint = postEndpoint;
        apacheDefaultClient = HttpClients.createDefault();
    }

    void notifySlackChannel() throws IOException {
        HttpPost httpPost = new HttpPost(postEndpoint);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-Type", "application/json");

        String inputJson = "{\"text\": \"Suomenlinnan lautoissa ongelmia!\"}";
        StringEntity stringEntity = new StringEntity(inputJson);
        httpPost.setEntity(stringEntity);

        HttpResponse response = apacheDefaultClient.execute(httpPost);


        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Can't post to monitoring api!");
        }
    }
}
