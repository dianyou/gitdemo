package hana;


import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class HANAConnection {
	
	private static Properties prop = new Properties();
	
	static
	{
		//get path for properties
		String fullpath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		StringBuilder path = new StringBuilder(fullpath);
		path.append("/").append("hanaInfo.properties");
		try {
			prop.load(new FileInputStream(path.toString()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Connection getConnection()
	{
		String hanaUser = prop.getProperty("HANA_USER");
		String hanaPasswd = prop.getProperty("HANA_PASSWD");
		String bpSchema = prop.getProperty("HANA_SCHEMA_BP");
		String bpTable = prop.getProperty("HANA_BP_TABLE");

		String hanaURL = new StringBuilder("jdbc:sap://").
				append(prop.getProperty("HANA_HOST")).append(":3").
				append(prop.getProperty("HANA_INSTANCE_NUMBER")).
					append("15/?autocommit=false&reconnect=true")
					.append("&currentschema=").append(bpSchema).toString();
	//	System.out.println(hanaURL);
		Connection con = null;
		//connect to hana
		try {
				Class.forName("com.sap.db.jdbc.Driver");
				con = DriverManager.getConnection(hanaURL,hanaUser,hanaPasswd);
			//	con = com.sap.db.jdbc.Driver.c
		} catch (ClassNotFoundException | SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
		}
		return con;
	}
	
	

}
