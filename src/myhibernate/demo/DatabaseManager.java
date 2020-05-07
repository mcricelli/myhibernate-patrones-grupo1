package myhibernate.demo;

import java.sql.*;

public class DatabaseManager {
    private final String DB_URL;
    private final String USER;
    private final String PASS;
    private Connection conn = null;

    public DatabaseManager(String db_url, String user, String pass){
        DB_URL = db_url;
        USER = user;
        PASS = pass;

        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void conectar(){
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void cerrar(){
        assert conn != null;
        try {
            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public ResultSet query(String q){
        ResultSet rs = null;
        Statement st = null;

        assert conn != null;
        try {
            st = conn.createStatement();
            rs = st.executeQuery(q);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            try {
                assert st != null;
                st.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return rs;
    }
}
