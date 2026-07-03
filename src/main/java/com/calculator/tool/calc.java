package com.calculator.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class calc {

    private static final Logger log = LoggerFactory.getLogger(calc.class);

    @McpTool(name ="add", description = "Add two numbers together")
    
    public int add(@McpToolParam(description = "First Number",required = true)int number1 , 
    @McpToolParam(description = "Second Number", required= true)int number2)
    {return number1 + number2;} /* basitçe toplama çıkartma yapan iki fonksiyon fonksiyonlara müdahalenin az olması amacıyla protected kullandım. */
    
    @McpTool(name = "sub", description = "Substruct two numbers between")
    public int sub(@McpToolParam(description = "First Number", required = true)int number1 , 
    @McpToolParam(description = "Second Number", required = true)int number2){return number1 - number2;}
}
 

