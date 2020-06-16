package com.mesilat.vbp.impl;

import com.atlassian.activeobjects.spi.DatabaseType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static com.mesilat.vbp.Constants.PLUGIN_KEY;
import static com.mesilat.vbp.Constants.REST_API_PATH;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.LoggerFactory;

public class FindResourceBase {
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PLUGIN_KEY);
    private static final String PAGE_INFO = "AO_C8A619_PAGE_INFO";

    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    protected final DatabaseType databaseType;

    protected ArrayNode find(final Connection conn, final String q, final int limit) throws SQLException, IOException{
        ArrayNode arr = mapper.createArrayNode();
        String sql;

        if (databaseType == DatabaseType.POSTGRESQL) {
            sql = (new StringBuilder())
                .append("SELECT C.CONTENTID, C.TITLE, D.\"DATA\"\n")
                .append("FROM \"").append(PAGE_INFO).append("\" D\n")
                .append("JOIN \"content\" C ON D.\"PAGE_ID\" = C.CONTENTID\n")
                .append("WHERE UPPER(C.TITLE) LIKE ?\n")
                .append("AND C.CONTENT_STATUS = 'current'\n")
                .append("ORDER BY TITLE\n")
                .append("LIMIT ")
                .append(limit)
                .toString();
        } else {
            sql = (new StringBuilder())
                .append("SELECT C.CONTENTID, C.TITLE, D.DATA\n")
                .append("FROM ").append(PAGE_INFO).append(" D\n")
                .append("JOIN CONTENT C ON D.PAGE_ID = C.CONTENTID\n")
                .append("WHERE UPPER(C.TITLE) LIKE ?\n")
                .append("AND C.CONTENT_STATUS = 'current'\n")
                .append("ORDER BY TITLE\n")
                .append("LIMIT ")
                .append(limit)
                .toString();
        }
        
        LOGGER.debug("Run SQL on Confluence DB: " + sql);

        try (
            PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            ps.setString(1, q == null? "%": String.format("%%%s%%", q.toUpperCase()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()){
                    ObjectNode obj = mapper.createObjectNode();
                    Long id = rs.getLong("CONTENTID");
                    obj.put("id", id);
                    obj.put("title", rs.getString("TITLE"));
                    obj.put("href", String.format("%s%s/page/%d", baseUrl, REST_API_PATH, id));
                    obj.put("view", String.format("%s/pages/viewpage.action?pageId=%d", baseUrl, id));
                    obj.set("data", mapper.readTree(rs.getString("DATA")));
                    arr.add(obj);
                }
            }
        }

        return arr;
    }
    protected ArrayNode find(final Connection conn, final String q, final List<String> labels, final int limit) throws SQLException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arr = mapper.createArrayNode();
        String sql;
        
        if (databaseType == DatabaseType.POSTGRESQL) {
            sql = (new StringBuilder())
                .append("SELECT C.CONTENTID, C.TITLE, D.\"DATA\"\n")
                .append("FROM \"").append(PAGE_INFO).append("\" D\n")
                .append("JOIN \"content\" C ON D.\"PAGE_ID\" = C.CONTENTID\n")
                .append("JOIN \"content_label\" CL ON CL.CONTENTID = C.CONTENTID\n")
                .append("JOIN \"label\" L ON CL.LABELID = L.LABELID\n")
                .append("WHERE L.NAME IN (\n")
                .append(String.join(",", labels.stream().map(label -> String.format("'%s'", label)).collect(Collectors.toList())))
                .append(")")
                .append("AND UPPER(C.TITLE) LIKE ?\n")
                .append("AND C.CONTENT_STATUS = 'current'\n")
                .append("ORDER BY TITLE\n")
                .append("LIMIT ")
                .append(limit)
                .toString();
        } else {
            sql = (new StringBuilder())
                .append("SELECT C.CONTENTID, C.TITLE, D.DATA\n")
                .append("FROM ").append(PAGE_INFO).append(" D\n")
                .append("JOIN CONTENT C ON D.PAGE_ID = C.CONTENTID\n")
                .append("JOIN CONTENT_LABEL CL ON CL.CONTENTID = C.CONTENTID\n")
                .append("JOIN LABEL L ON CL.LABELID = L.LABELID\n")
                .append("WHERE L.NAME IN (\n")
                .append(String.join(",", labels.stream().map(label -> String.format("'%s'", label)).collect(Collectors.toList())))
                .append(")")
                .append("AND UPPER(C.TITLE) LIKE ?\n")
                .append("AND C.CONTENT_STATUS = 'current'\n")
                .append("ORDER BY TITLE\n")
                .append("LIMIT ")
                .append(limit)
                .toString();
        }

        LOGGER.debug("Run SQL on Confluence DB: " + sql);
        
        try (
            PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            ps.setString(1, q == null? "%": String.format("%%%s%%", q.toUpperCase()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()){
                    ObjectNode obj = mapper.createObjectNode();
                    Long id = rs.getLong("CONTENTID");
                    obj.put("id", id);
                    obj.put("title", rs.getString("TITLE"));
                    obj.put("href", String.format("%s%s/page/%d", baseUrl, REST_API_PATH, id));
                    obj.put("view", String.format("%s/pages/viewpage.action?pageId=%d", baseUrl, id));
                    obj.set("data", mapper.readTree(rs.getString("DATA")));
                    arr.add(obj);
                }
            }
        }

        return arr;
    }
    protected ArrayNode find2(final Connection conn, final String q, final List<String> labels, final int limit) throws SQLException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arr = mapper.createArrayNode();
        String sql;

        if (databaseType == DatabaseType.POSTGRESQL) {
            sql = (new StringBuilder())
                .append("SELECT C.CONTENTID, C.TITLE, D.\"DATA\"\n")
                .append("FROM \"").append(PAGE_INFO).append("\" D\n")
                .append("JOIN \"content\" C ON D.\"PAGE_ID\" = C.CONTENTID\n")
                .append("JOIN \"content_label\" CL ON CL.CONTENTID = C.CONTENTID\n")
                .append("JOIN \"label\" L ON CL.LABELID = L.LABELID\n")
                .append("WHERE L.NAME IN (\n")
                .append(String.join(",", labels.stream().map(label -> String.format("'%s'", label)).collect(Collectors.toList())))
                .append(")")
                .append("AND UPPER(D.\"DATA\") LIKE ?\n")
                .append("AND C.CONTENT_STATUS = 'current'\n")
                .append("UNION\n")
                .append("SELECT C.CONTENTID, C.TITLE, D.\"DATA\"\n")
                .append("FROM \"").append(PAGE_INFO).append("\" D\n")
                .append("JOIN \"content\" C ON D.\"PAGE_ID\" = C.CONTENTID\n")
                .append("JOIN \"content_label\" CL ON CL.CONTENTID = C.CONTENTID\n")
                .append("JOIN \"label\" L ON CL.LABELID = L.LABELID\n")
                .append("WHERE L.NAME IN (\n")
                .append(String.join(",", labels.stream().map(label -> String.format("'%s'", label)).collect(Collectors.toList())))
                .append(")")
                .append("AND UPPER(C.TITLE) LIKE ?\n")
                .append("AND C.CONTENT_STATUS = 'current'\n")
                .append("ORDER BY TITLE\n")
                .append("LIMIT ")
                .append(limit)
                .toString();
        } else {
            sql = (new StringBuilder())
                .append("SELECT C.CONTENTID, C.TITLE, D.DATA\n")
                .append("FROM ").append(PAGE_INFO).append(" D\n")
                .append("JOIN CONTENT C ON D.PAGE_ID = C.CONTENTID\n")
                .append("JOIN CONTENT_LABEL CL ON CL.CONTENTID = C.CONTENTID\n")
                .append("JOIN LABEL L ON CL.LABELID = L.LABELID\n")
                .append("WHERE L.NAME IN (\n")
                .append(String.join(",", labels.stream().map(label -> String.format("'%s'", label)).collect(Collectors.toList())))
                .append(")")
                .append("AND UPPER(D.DATA) LIKE ?\n")
                .append("AND C.CONTENT_STATUS = 'current'\n")
                .append("UNION\n")
                .append("SELECT C.CONTENTID, C.TITLE, D.DATA\n")
                .append("FROM ").append(PAGE_INFO).append(" D\n")
                .append("JOIN CONTENT C ON D.PAGE_ID = C.CONTENTID\n")
                .append("JOIN CONTENT_LABEL CL ON CL.CONTENTID = C.CONTENTID\n")
                .append("JOIN LABEL L ON CL.LABELID = L.LABELID\n")
                .append("WHERE L.NAME IN (\n")
                .append(String.join(",", labels.stream().map(label -> String.format("'%s'", label)).collect(Collectors.toList())))
                .append(")")
                .append("AND UPPER(C.TITLE) LIKE ?\n")
                .append("AND C.CONTENT_STATUS = 'current'\n")
                .append("ORDER BY TITLE\n")
                .append("LIMIT ")
                .append(limit)
                .toString();
        }
        

        LOGGER.debug("Run SQL on Confluence DB: " + sql);
        
        try (
            PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            String _q = q == null? "%": String.format("%%%s%%", q.toUpperCase());
            ps.setString(1, _q);
            ps.setString(2, _q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()){
                    ObjectNode obj = mapper.createObjectNode();
                    Long id = rs.getLong("CONTENTID");
                    obj.put("id", id);
                    obj.put("title", rs.getString("TITLE"));
                    obj.put("href", String.format("%s%s/page/%d", baseUrl, REST_API_PATH, id));
                    obj.put("view", String.format("%s/pages/viewpage.action?pageId=%d", baseUrl, id));
                    obj.set("data", mapper.readTree(rs.getString("DATA")));
                    arr.add(obj);
                }
            }
        }

        return arr;
    }

    protected String getSchema(String url) {
        try {
            if (url == null)
                return null;
            if (!url.startsWith("jdbc:"))
                return null;
            URI uri = new URI(url.substring(5));
            MutableObject<String> currentSchema = new MutableObject<>();
            Pattern.compile("&")
            .splitAsStream(uri.getQuery())
            .forEach(s -> {
                String[] aa = s.split("=");
                if ("currentSchema".equals(aa[0].trim())) {
                    currentSchema.setValue(aa[1].trim());
                }
            });
            return currentSchema.getValue();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    public FindResourceBase(String baseUrl, DatabaseType databaseType){
        this.baseUrl = baseUrl;
        this.databaseType = databaseType;
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
}
