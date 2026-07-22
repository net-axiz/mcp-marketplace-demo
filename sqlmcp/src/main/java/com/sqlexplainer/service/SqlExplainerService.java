package com.sqlexplainer.service;

import com.sqlexplainer.model.SqlQueryParts;
import org.springframework.stereotype.Service;

@Service
public class SqlExplainerService {

    private final SqlParser sqlParser;

    public SqlExplainerService(SqlParser sqlParser) {
        this.sqlParser = sqlParser;
    }

    public String explain(String sqlQuery) {
        SqlQueryParts parts = sqlParser.parse(sqlQuery);
        
        if (parts.condition() == null) {
            return String.format("This query retrieves %s data from the %s table.", parts.columns(), parts.table());
        }

        return String.format("This query retrieves %s data from the %s table where %s.", parts.columns(), parts.table(), parts.condition());
    }
}