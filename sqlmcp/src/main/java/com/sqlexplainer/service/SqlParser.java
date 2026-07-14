package com.sqlexplainer.service;

import com.sqlexplainer.model.SqlQueryParts;

public interface SqlParser {
    SqlQueryParts parse(String sqlQuery);
}
