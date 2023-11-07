package com.iisquare.fs.app.flink.core;

import com.iisquare.fs.base.dag.DAGCore;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;

import java.util.Date;
import java.util.Map;

public class FlinkCore {

    public static final String TYPE_FIELD = "flink";
    public static Map<String, Map<String, Object>> clsTypes = DAGCore.clsTypes;

    static {
        clsTypes.get(Boolean.class.getName()).put(TYPE_FIELD, Types.BOOLEAN);
        clsTypes.get(Byte.class.getName()).put(TYPE_FIELD, Types.BYTE);
        clsTypes.get(Double.class.getName()).put(TYPE_FIELD, Types.DOUBLE);
        clsTypes.get(Float.class.getName()).put(TYPE_FIELD, Types.FLOAT);
        clsTypes.get(Integer.class.getName()).put(TYPE_FIELD, Types.INT);
        clsTypes.get(Long.class.getName()).put(TYPE_FIELD, Types.LONG);
        clsTypes.get(Short.class.getName()).put(TYPE_FIELD, Types.SHORT);
        clsTypes.get(String.class.getName()).put(TYPE_FIELD, Types.STRING);
        clsTypes.get(Date.class.getName()).put(TYPE_FIELD, Types.SQL_DATE);
        clsTypes.get(java.sql.Date.class.getName()).put(TYPE_FIELD, Types.SQL_DATE);
        clsTypes.get(java.sql.Timestamp.class.getName()).put(TYPE_FIELD, Types.SQL_TIMESTAMP);
    }

    public static TypeInformation type(String type) {
        if (!clsTypes.containsKey(type)) return null;
        return (TypeInformation) clsTypes.get(type).get(TYPE_FIELD);
    }

}
