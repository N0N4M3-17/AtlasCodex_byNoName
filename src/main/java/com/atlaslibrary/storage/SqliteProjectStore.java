package com.atlaslibrary.storage;

import com.atlaslibrary.domain.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public class SqliteProjectStore {
    public void initSchema(Path dbPath) throws Exception {
        Files.createDirectories(dbPath.getParent());
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS layers(id TEXT PRIMARY KEY, name TEXT, z_order INTEGER, visible INTEGER, locked INTEGER, opacity REAL)");
                st.execute("CREATE TABLE IF NOT EXISTS nodes(id TEXT PRIMARY KEY, type TEXT, layer_id TEXT, x REAL, y REAL, scale REAL, rotation REAL, opacity REAL, visible INTEGER, locked INTEGER, content TEXT, width REAL, height REAL, font_size REAL)");
                st.execute("CREATE TABLE IF NOT EXISTS articles(id TEXT PRIMARY KEY, slug TEXT, title TEXT, markdown_body TEXT)");
                st.execute("CREATE TABLE IF NOT EXISTS anchors(id TEXT PRIMARY KEY, source_node_id TEXT, type TEXT, target TEXT)");
            }
        }
    }

    public void save(Path dbPath, ProjectState project) throws Exception {
        initSchema(dbPath);
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) { st.execute("DELETE FROM layers"); st.execute("DELETE FROM nodes"); st.execute("DELETE FROM articles"); st.execute("DELETE FROM anchors"); }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO layers VALUES(?,?,?,?,?,?)")) { for (Layer l:project.layers){ps.setString(1,l.id);ps.setString(2,l.name);ps.setInt(3,l.zOrder);ps.setInt(4,l.visible?1:0);ps.setInt(5,l.locked?1:0);ps.setDouble(6,l.opacity);ps.addBatch();} ps.executeBatch(); }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO nodes VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                for (AtlasNode n:project.nodes){ps.setString(1,n.id);ps.setString(2,n.type.name());ps.setString(3,n.layerId);ps.setDouble(4,n.x);ps.setDouble(5,n.y);ps.setDouble(6,n.scale);ps.setDouble(7,n.rotation);ps.setDouble(8,n.opacity);ps.setInt(9,n.visible?1:0);ps.setInt(10,n.locked?1:0);ps.setString(11,n.content);ps.setDouble(12,n.width);ps.setDouble(13,n.height);ps.setDouble(14,n.fontSize);ps.addBatch();}
                ps.executeBatch();
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO articles VALUES(?,?,?,?)")) { for (Article a:project.articles){ps.setString(1,a.id);ps.setString(2,a.slug);ps.setString(3,a.title);ps.setString(4,a.markdownBody);ps.addBatch();} ps.executeBatch(); }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO anchors VALUES(?,?,?,?)")) { for (Anchor a:project.anchors){ps.setString(1,a.id);ps.setString(2,a.sourceNodeId);ps.setString(3,a.type.name());ps.setString(4,a.target);ps.addBatch();} ps.executeBatch(); }
            c.commit();
        }
    }

    public ProjectState load(Path dbPath) throws Exception {
        ProjectState p = new ProjectState();
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            try (ResultSet rs = c.createStatement().executeQuery("SELECT * FROM layers ORDER BY z_order")) { while(rs.next()){Layer l=new Layer(rs.getString("name"),rs.getInt("z_order")); l.id=rs.getString("id"); l.visible=rs.getInt("visible")==1; l.locked=rs.getInt("locked")==1; l.opacity=rs.getDouble("opacity"); p.layers.add(l);} }
            try (ResultSet rs = c.createStatement().executeQuery("SELECT * FROM nodes")) { while(rs.next()){AtlasNode n=new AtlasNode(); n.id=rs.getString("id"); n.type=NodeType.valueOf(rs.getString("type")); n.layerId=rs.getString("layer_id"); n.x=rs.getDouble("x"); n.y=rs.getDouble("y"); n.scale=rs.getDouble("scale"); n.rotation=rs.getDouble("rotation"); n.opacity=rs.getDouble("opacity"); n.visible=rs.getInt("visible")==1; n.locked=rs.getInt("locked")==1; n.content=rs.getString("content"); n.width=rs.getDouble("width"); n.height=rs.getDouble("height"); n.fontSize=rs.getDouble("font_size"); p.nodes.add(n);} }
            try (ResultSet rs = c.createStatement().executeQuery("SELECT * FROM articles")) { while(rs.next()){Article a=new Article(); a.id=rs.getString("id"); a.slug=rs.getString("slug"); a.title=rs.getString("title"); a.markdownBody=rs.getString("markdown_body"); p.articles.add(a);} }
            try (ResultSet rs = c.createStatement().executeQuery("SELECT * FROM anchors")) { while(rs.next()){Anchor a=new Anchor(); a.id=rs.getString("id"); a.sourceNodeId=rs.getString("source_node_id"); a.type=AnchorType.valueOf(rs.getString("type")); a.target=rs.getString("target"); p.anchors.add(a);} }
        }
        return p;
    }
}
