package com.loohp.hkweatherwarnings.cache;

import org.json.JSONException;
import org.json.JSONObject;

public interface JSONSerializable {

    JSONObject serialize() throws JSONException;

}
