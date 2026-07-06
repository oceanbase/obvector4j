package com.oceanbase.obvector4j.util;

import com.oceanbase.obvector4j.schema.DataType;
import java.sql.Types;

/**
 * Maps JDBC column metadata to SDK {@link DataType}.
 */
public final class JdbcTypeMapper {

    private JdbcTypeMapper() {
    }

    public static DataType fromJdbc(int jdbcType, String typeName) {
        if (typeName != null) {
            String upper = typeName.toUpperCase();
            if (upper.contains("VECTOR") || upper.contains("FLOAT_VECTOR")) {
                return DataType.FLOAT_VECTOR;
            }
            if (upper.contains("TINYINT") || upper.contains("BOOLEAN")) {
                return DataType.BOOL;
            }
            if (upper.contains("SMALLINT")) {
                return DataType.INT16;
            }
            if (upper.contains("BIGINT")) {
                return DataType.INT64;
            }
            if (upper.contains("INT")) {
                return DataType.INT32;
            }
            if (upper.contains("FLOAT") && !upper.contains("VECTOR")) {
                return DataType.FLOAT;
            }
            if (upper.contains("DOUBLE") || upper.contains("DECIMAL") || upper.contains("NUMERIC")) {
                return DataType.DOUBLE;
            }
            if (upper.contains("JSON")) {
                return DataType.JSON;
            }
            if (upper.contains("VARCHAR") || upper.contains("CHAR") || upper.contains("TEXT")) {
                return DataType.STRING;
            }
        }

        switch (jdbcType) {
            case Types.TINYINT:
            case Types.BIT:
                return DataType.BOOL;
            case Types.SMALLINT:
                return DataType.INT16;
            case Types.INTEGER:
                return DataType.INT32;
            case Types.BIGINT:
                return DataType.INT64;
            case Types.FLOAT:
            case Types.REAL:
                return DataType.FLOAT;
            case Types.DOUBLE:
            case Types.DECIMAL:
            case Types.NUMERIC:
                return DataType.DOUBLE;
            default:
                return DataType.STRING;
        }
    }
}
