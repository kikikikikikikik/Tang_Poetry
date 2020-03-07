package Lab;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import javax.sql.DataSource;
import java.sql.*;

public class 插入诗词Demo {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        String 朝代 = "唐代";
        String 作者 = "白居易";
        String 标题 = "问刘十九";
        String 正文 = "绿蚁新醅酒，红泥小火炉。晚来天欲雪，能饮一杯无？";
        //1.注册Driver
        //2.获取Connection通过DriverManager
        Class.forName("com.mysql.jdbc.Driver");
        String url = "jdbc:mysql://127.0.0.1/tangshi?useSSL=false&charactrEncodeing=utf8";
        //Connection connection = DriverManager.getConnection(url,"root","1002");
        // System.out.println(connection);
        //通过DataSource获取Connection
        //DataSource dataSource = new MysqlDataSource();
        //带有连接池，好处参照线程池
        MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
        dataSource.setServerName("127.0.0.1");
        dataSource.setPort(3306);
        dataSource.setUser("root");
        dataSource.setPassword("1002");
        dataSource.setDatabaseName("tangshi");
        dataSource.setUseSSL(false);
        dataSource.setCharacterEncoding("UTF8");

        try(Connection connection = dataSource.getConnection()){
            String sql = "insert into tangshi" +
                    " (sha256,dynasty,title,author,content,words)"+
                    "values (?,?,?,?,?,?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1,"sha256");
            statement.setString(2,朝代);
            statement.setString(3,标题);
            statement.setString(4,作者);
            statement.setString(5,正文);
            statement.setString(6,"");
            statement.executeUpdate();
        }
    }
}
