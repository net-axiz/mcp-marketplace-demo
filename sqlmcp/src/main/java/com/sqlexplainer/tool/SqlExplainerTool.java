package com.sqlexplainer.tool;

import com.sqlexplainer.service.SqlExplainerService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class SqlExplainerTool {
    
    private final SqlExplainerService sqlExplainerService;

    public SqlExplainerTool(SqlExplainerService sqlExplainerService) {
        this.sqlExplainerService = sqlExplainerService;
    }

    @McpTool(name = "SQL Query Explainer", description = "Explains the job of given SQL Query")
    public String explainSQLquery(@McpToolParam(description= "SQL Query", required = true) String sqlText) {
        return sqlExplainerService.explain(sqlText);
    } 
}
