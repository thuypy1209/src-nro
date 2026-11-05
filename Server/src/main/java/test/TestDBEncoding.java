package test;

import java.sql.*;

public class TestDBEncoding {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/nro?characterEncoding=utf8&useUnicode=true";
        Connection conn = DriverManager.getConnection(url, "root", "");
        
        System.out.println("--- TASK ---");
        PreparedStatement ps = conn.prepareStatement("SELECT id, name, detail FROM task_main_template WHERE id = 28");
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            System.out.println("Name: " + rs.getString("name"));
            System.out.println("Detail: " + rs.getString("detail"));
        }
        rs.close();
        ps.close();

        System.out.println("--- INTRINSIC ---");
        ps = conn.prepareStatement("SELECT id, name FROM intrinsic WHERE id = 0");
        rs = ps.executeQuery();
        if (rs.next()) {
            System.out.println("Name: " + rs.getString("name"));
        }
        rs.close();
        ps.close();

        System.out.println("--- MAP ---");
        ps = conn.prepareStatement("SELECT id, name FROM map_template WHERE id = 100");
        rs = ps.executeQuery();
        if (rs.next()) {
            System.out.println("Name: " + rs.getString("name"));
        }
        rs.close();
        ps.close();
        conn.close();
    }
}
