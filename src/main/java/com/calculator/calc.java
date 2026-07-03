package com.calculator.CalcMcpServerApplication.tools;

import org.springframework.ai.mcp.server.annotation.McpTool;
import org.springframework.ai.mcp.server.annotation.McpToolParam;
import org.springframework.stereotype.Component;


@Component
public class calc {
    @Mcptool(name ="add", description = "Add two numbers together")
    
    protected int add(@McpToolParam(description = "First Number",required = true)int number1 , 
    @McpToolParam(description = "Second Number", required= true)int number2)
    {return number1 + number2;} /* basitçe toplama çıkartma yapan iki fonksiyon fonksiyonlara müdahalenin az olması amacıyla protected kullandım. */
    
    @Mcptool(name = "sub", description = "Substruct two numbers between")
    protected int sub(@McpToolParam(description = "First Number", required = true)int number1 , 
    @McpToolParam(description = "Second Number", required = true)int number2){return number1 - number2;}
}
 

