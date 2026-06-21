package config;
import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

public class koneksi {
    public static Connection koneksiDB() { 
        try {
            String url = "jdbc:mysql://localhost:3306/pis12noliga";
            String user = "root";
            String pass = "";
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());

            Connection conn = DriverManager.getConnection(url, user, pass);
            return conn;
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Koneksi Database Gagal: " + e.getMessage());
            return null;
        }
    }
}
