package test;

import nro.jdbc.DBHika;
import nro.models.task.TaskMain;
import nro.server.io.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestMessageBytes {
    public static void main(String[] args) throws Exception {
        Connection con = DBHika.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT name, detail FROM task_main_template WHERE id = 28");
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String name = rs.getString("name");
            String detail = rs.getString("detail");
            System.out.println("Read from DB: name = " + name);
            System.out.println("Read from DB: detail = " + detail);

            Message msg = new Message(40);
            msg.writer().writeUTF(name);
            msg.writer().writeUTF(detail);
            byte[] data = msg.getData();
            System.out.print("Message bytes: ");
            for (byte b : data) {
                System.out.print(String.format("%02X ", b));
            }
            System.out.println();
            
            // Now decode bytes with UTF-8
            // Message format: cmd (none in getData, os holds body), length (2 bytes), utf bytes
            int len1 = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            String decName = new String(data, 2, len1, "UTF-8");
            System.out.println("Decoded name: " + decName);
            
            int offset2 = 2 + len1;
            int len2 = ((data[offset2] & 0xFF) << 8) | (data[offset2+1] & 0xFF);
            String decDetail = new String(data, offset2 + 2, len2, "UTF-8");
            System.out.println("Decoded detail: " + decDetail);
        }
        con.close();
        System.exit(0);
    }
}
