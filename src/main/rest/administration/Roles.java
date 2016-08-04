package administration;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import hana.HANAConnection;

@Path("admin")
public class Roles {
	
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
	
	@GET
	@Path("rolelist")
	/*
	 * http://localhost:8080/businesspartner/rest/admin/rolelist
	 */
	public Response roles()
	{
		Connection conn = HANAConnection.getConnection(); 
		String sql = "select * from BP_ROLE_COLLECTIONS order by collections";
		Statement stmt = null;
		
		JSONArray ja = new JSONArray();
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				JSONObject jo = new JSONObject();
				jo.put("RoleId",rs.getString("ID"));
				jo.put("Collections",rs.getString("COLLECTIONS"));
				ja.put(jo);
			}
			rs.close();
			stmt.close();
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JSONObject res = new JSONObject();
		res.put("RoleList",ja);
		
		Response response = Response.status(200).
                entity(res.toString()).
                header("Content-Type", "application/json; charset=utf-8").build();
		return response;
	}
	

}
