package fi.hsl.suomenlinna_hfp;

import ch.qos.logback.contrib.json.JsonFormatter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

public class GsonLogFormatter implements JsonFormatter {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public String toJsonString(Map m) {
        return gson.toJson(m);
    }
}
