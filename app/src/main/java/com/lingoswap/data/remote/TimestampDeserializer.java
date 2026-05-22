package com.lingoswap.data.remote;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.lingoswap.data.model.Message;

import java.lang.reflect.Type;

/**
 * Custom deserializer cho Message.TimestampField.
 */
public class TimestampDeserializer implements JsonDeserializer<Message.TimestampField> {

    @Override
    public Message.TimestampField deserialize(JsonElement json, Type typeOfT,
                                               JsonDeserializationContext ctx)
            throws JsonParseException {

        if (json.isJsonPrimitive()) {
            // Socket trả về ISO string
            return new Message.TimestampField(json.getAsString());
        }

        if (json.isJsonObject()) {
            // REST trả về { full, friendly }
            JsonObject obj = json.getAsJsonObject();
            Message.TimestampField ts = new Message.TimestampField();
            if (obj.has("full"))     ts.setFull(obj.get("full").getAsString());
            if (obj.has("friendly")) ts.setFriendly(obj.get("friendly").getAsString());
            return ts;
        }

        return new Message.TimestampField();
    }
}
