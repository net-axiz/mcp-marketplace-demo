package com.sqlexplainer.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SqlExplainerService {

    private static final Pattern FROM_PATTERN = Pattern.compile("FROM\s+([a-zA-Z][a-zA-Z0-9_]*)",Pattern.CASE_INSENSITIVE |Pattern.DOTALL);
    private static final Pattern SELECT_PATTERN = Pattern.compile("SELECT\s+(.+?)\s+FROM",Pattern.CASE_INSENSITIVE |Pattern.DOTALL);
    private static final Pattern WHERE_PATTERN = Pattern.compile("WHERE\s+(.+?)(?:ORDER BY|GROUP BY|LIMIT|;|$)",Pattern.CASE_INSENSITIVE |Pattern.DOTALL);


    public static String explain(String sqlQuery) {
        String table = extractTable(sqlQuery);
        String columns = extractColumns(sqlQuery);
        String condition = extractWhere(sqlQuery);
    
        if(condition == null){return String.format("Bu sorgu %s tablosundan %s verisini çeker.",table ,columns);}

        return String.format("Bu sorgu %s tablosundan, %s koşuluna uyan %s verisini çeker.", table, condition,columns);}

    private static String extractTable(String sql){
        Matcher matcher = FROM_PATTERN.matcher(sql);
        return matcher.find() ? matcher.group(1) : "Not in regex Domain";}

    private static String extractColumns(String sql) {
        Matcher matcher = SELECT_PATTERN.matcher(sql);
        if (!matcher.find()) {return "Not in regex Domain";}
        String columns = matcher.group(1).trim();
        return  columns.equals("*") ? "All" : columns;}

    private static String extractWhere(String sql){
        Matcher matcher = WHERE_PATTERN.matcher(sql);
        return matcher.find() ? matcher.group(1): null;}}