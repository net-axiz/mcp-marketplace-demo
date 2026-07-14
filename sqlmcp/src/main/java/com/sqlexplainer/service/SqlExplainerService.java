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
            return String.format("Bu sorgu %s tablosundan %s verisini çeker.", parts.table(), parts.columns());
        }

        return String.format("Bu sorgu %s tablosundan, %s koşuluna uyan %s verisini çeker.", parts.table(), parts.condition(), parts.columns());
    }
}