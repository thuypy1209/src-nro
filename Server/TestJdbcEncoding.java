import java.sql.*;

public class TestJdbcEncoding {
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url1 = "jdbc:mysql://localhost:3306/nro";
            testConnection(url1, "Without params");

            String url2 = "jdbc:mysql://localhost:3306/nro?useUnicode=true&characterEncoding=UTF-8&characterSetResults=UTF-8";
            testConnection(url2, "With UTF-8 params");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testConnection(String url, String desc) throws Exception {
        System.out.println("=== " + desc + ": " + url + " ===");
        try (Connection con = DriverManager.getConnection(url, "root", "")) {
            try (Statement st = con.createStatement()) {
                ResultSet rs = st.executeQuery("SELECT name FROM task_main_template WHERE id = 28");
                if (rs.next()) {
                    String name = rs.getString(1);
                    System.out.println("Task 28 name: " + name);
                    for (char c : name.toCharArray()) {
                        System.out.print(String.format("\\u%04x", (int) c));
                    }
                    System.out.println();
                }
                rs = st.executeQuery("SELECT name FROM map_template WHERE id = 103");
                if (rs.next()) {
                    String name = rs.getString(1);
                    System.out.println("Map 103 name: " + name);
                    for (char c : name.toCharArray()) {
                        System.out.print(String.format("\\u%04x", (int) c));
                    }
                    System.out.println();
                }
            }
        }
    }
}
