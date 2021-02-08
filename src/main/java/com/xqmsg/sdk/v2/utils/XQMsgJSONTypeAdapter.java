package com.xqmsg.sdk.v2.utils;

/**
 * @author Jan Abt
 * @date Jan 10, 2021
 */


import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.LinkedTreeMap;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapts types whose static type is only 'Object'. Uses getClass() on
 * serialization and a primitive/Map/List on deserialization.
 */
public final class XQMsgJSONTypeAdapter implements JsonDeserializer<Map<String, Object>> {

  @Override  @SuppressWarnings("unchecked")
  public Map<String, Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return (Map<String, Object>) read(json);
  }

  public Object read(JsonElement in) {

    if(in.isJsonArray()){
      List<Object> list = new ArrayList<Object>();
      JsonArray arr = in.getAsJsonArray();
      for (JsonElement anArr : arr) {
        list.add(read(anArr));
      }
      return list;
    }else if(in.isJsonObject()){
      Map<String, Object> map = new LinkedTreeMap<String, Object>();
      JsonObject obj = in.getAsJsonObject();
      Set<Map.Entry<String, JsonElement>> entitySet = obj.entrySet();
      for(Map.Entry<String, JsonElement> entry: entitySet){
        map.put(entry.getKey(), read(entry.getValue()));
      }
      return map;
    }else if( in.isJsonPrimitive()){
      JsonPrimitive prim = in.getAsJsonPrimitive();
      if(prim.isBoolean()){
        return prim.getAsBoolean();
      }else if(prim.isString()){
        return prim.getAsString();
      }else if(prim.isNumber()){

        Number num = prim.getAsNumber();
        //only render as Long od Double
     // if(Math.ceil(num.intValue()) == num.intValue())
     //   return num.intValue();
     // else
        if(Math.ceil(num.doubleValue()) == num.longValue())
          return num.longValue();
        else{
          return num.doubleValue();
        }
      }
    }
    return null;
  }
}