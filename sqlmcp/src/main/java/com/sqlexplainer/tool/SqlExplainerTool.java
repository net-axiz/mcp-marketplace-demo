package com.sqlexplainer.tool;

import com.sqlexplainer.service.SqlExplainerService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class SqlExplainerTool {
    @McpTool(name = "SQL Query Explainer", description = "Explains the job of given SQL Query")
    public String  explainSQLquery(@McpToolParam(description= "SQL Query", required = true)String sqlText)
        {return SqlExplainerService.explain(sqlText);} 
}
