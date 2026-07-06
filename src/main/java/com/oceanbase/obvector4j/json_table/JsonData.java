package com.oceanbase.obvector4j.json_table;

public abstract class JsonData {
    public JsonDataType dataType;
    public abstract String toJson();
}
