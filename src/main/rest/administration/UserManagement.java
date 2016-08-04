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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import hana.HANAConnection;

@Path("admin")
public class UserManagement {
	
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
	@Path("userinfo")
	/*
	 * http://localhost:8080/businesspartner/rest/admin/userInfo?ssoid=user1@saptt.com
	 */
	public Response userInfo(@QueryParam("SSOID")String ssoid)
	{
		JSONObject jo = getUserInfo(ssoid);
		Response response = Response.status(200).
                entity(jo.toString()).
                header("Content-Type", "application/json; charset=utf-8").build();
		return response;
		
	}
	private JSONObject getUserInfo(String ssoid)
	{
		Connection conn = HANAConnection.getConnection(); 
		String sql = "select * from BP_MEMBER where ssoid=?";
		JSONObject jo = new JSONObject();
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, ssoid);
			ResultSet rs = pstmt.executeQuery();
			
			if(rs.next())
			{
				jo.put("SSOID", ssoid);
				jo.put("FirstName", rs.getString("FIRSTNAME"));
				jo.put("LastName", rs.getString("LASTNAME"));
				jo.put("Sex", rs.getString("SEX"));
				jo.put("BP", rs.getString("BP"));
				jo.put("Email", rs.getString("EMAIL"));
			}
			rs.close();
			pstmt.close();
			conn.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jo;
	}
	
	@GET
	@Path("users")
	/*
	 * http://localhost:8080/businesspartner/rest/admin/users
	 */
	public Response users()
	{
		JSONObject jo = getAllUsers();
		Response response = Response.status(200).
                entity(jo.toString()).
                header("Content-Type", "application/json; charset=utf-8").build();
		return response;
	}
	
	private JSONObject getAllUsers()
	{
		Connection conn = HANAConnection.getConnection(); 
		String sql = "select * from BP_MEMBER order by FIRSTNAME,LASTNAME";
				
		Statement stmt = null;
		
		JSONArray ja = new JSONArray();
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				JSONObject jo = new JSONObject();
				jo.put("SSOID", rs.getString("SSOID"));
				jo.put("FirstName", rs.getString("FIRSTNAME"));
				jo.put("LastName", rs.getString("LASTNAME"));
				jo.put("Sex", rs.getString("SEX"));
				jo.put("BP", rs.getString("BP"));
				jo.put("Email", rs.getString("EMAIL"));
				
				String collectionSQL = "select * from BP_USER_ROLE where ssoid=?";
				PreparedStatement pstmt=conn.prepareStatement(collectionSQL);
				pstmt.setString(1, jo.getString("SSOID"));
				ResultSet colRS = pstmt.executeQuery();
				JSONArray collections = new JSONArray();
				while(colRS.next())
				{
					collections.put(colRS.getString("COLLECTIONS"));
				}
				colRS.close();
				pstmt.close();
				
				jo.put("Collections", collections);
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
		res.put("UserInfo",ja);
		return res;
	}
	
	@POST
	@Path("/createuser")
	/*
	 * http://localhost:8080/businesspartner/rest/admin/createUser
	 {
		"SSOID":"user1@saptt.com",
		"FirstName":"Haopeng",
		"LastName":"Men",
		"Email":"gavin.gai@sap.com",
		"BP":"Lenovo",
		"Sex":"Male",
		"Collections":["Transportation Planner","Outbound Delivery Manager","Inbound Supply Chain Planner"]
	}
	  
	 */
	public Response create(String s)
	{
		JSONObject jo = new JSONObject(s);
		boolean flag =createUser(jo);
		
		if(flag)
		{
			JSONObject res = new JSONObject();
			res.put("data", "success");
			Response response = Response.status(200).
	                entity(res.toString()).
	                header("Content-Type", "application/json; charset=utf-8").build();
			return response;
		}
		else
		{
			JSONObject res = new JSONObject();
			res.put("data", "failed");
			Response response = Response.status(400)
					.entity(res.toString()).header("Content-Type", 
							"application/json; charset=utf-8").build();
			return response;
		}

	}
	private boolean createUser(JSONObject jo)
	{
		String firstName = jo.getString("FirstName");
		String lastName = jo.getString("LastName");
		String email = jo.getString("Email");
		String sex = jo.getString("Sex");
		String bpName = jo.getString("BP");
		JSONArray ja = jo.getJSONArray("Collections");
		Connection conn = HANAConnection.getConnection(); 
		String addBPMember= "insert into BP_MEMBER("
				+ "SSOID,FIRSTNAME,LASTNAME,BP,SEX,EMAIL,JOINEDTIME)"
				+ " values(?,?,?,?,?,?,current_timestamp)";

		PreparedStatement pstmt;
		PreparedStatement rpstmt;
		try {
			pstmt = conn.prepareStatement(addBPMember);
			pstmt.setString(1, email);
			pstmt.setString(2, firstName);
			pstmt.setString(3, lastName);
			pstmt.setString(4, bpName);
			pstmt.setString(5, sex);
			pstmt.setString(6, email);
			pstmt.execute();

			String user_map_role = "insert into BP_USER_ROLE(ssoid,collections)"
					+ " values(?,?)";
			rpstmt = conn.prepareStatement(user_map_role);
			for(int i=0;i<ja.length();i++)
			{
				rpstmt.setString(1, email);
				rpstmt.setString(2, ja.getString(i));
				rpstmt.execute();
			}
			rpstmt.close();
			pstmt.close();
			conn.commit();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
		
	}
	
	@POST
	@Path("/edituser")
	/*
	 * http://localhost:8080/businesspartner/rest/admin/editUser
	 	{
			"SSOID":"user1@saptt.com",
			"FirstName":"Wen",
			"LastName":"Gai",
			"Email":"gavin.gai@sap.com",
			"Sex":"Male",
			"Collections":["Transportation Planner","Outbound Delivery Manager","Inbound Supply Chain Planner"]
		}
	 */
	public Response edit(String s)
	{
		JSONObject jo = new JSONObject(s);
		boolean flag = editUser(jo);
		if(flag)
		{
			JSONObject res = new JSONObject();
			res.put("data", "success");
			Response response = Response.status(200).
	                entity(res.toString()).
	                header("Content-Type", "application/json; charset=utf-8").build();
			return response;
		}
		else
		{
			JSONObject res = new JSONObject();
			res.put("data", "failed");
			Response response = Response.status(400)
					.entity(res.toString()).header("Content-Type", 
							"application/json; charset=utf-8").build();
			return response;
		}		
	}
	
	private boolean editUser(JSONObject jo)
	{
		String ssoid = jo.getString("SSOID");
		String firstName = jo.getString("FirstName");
		String lastName = jo.getString("LastName");
		String email = jo.getString("Email");
		String sex = jo.getString("Sex");
		JSONArray ja = jo.getJSONArray("Collections");
		Connection conn = HANAConnection.getConnection();
		
		try {
			
			String delete_user_role = "delete from BP_USER_ROLE where ssoid=?";
			PreparedStatement dpstmt = conn.prepareStatement(delete_user_role);
			dpstmt.setString(1, ssoid);
			dpstmt.execute();
			dpstmt.close();
			
			String updateBPMember= "update BP_MEMBER set "
					+ "FIRSTNAME=?,LASTNAME=?,SEX=?,EMAIL=? "
					+ "where SSOID=?";
			
			PreparedStatement pstmt = conn.prepareStatement(updateBPMember);
			pstmt.setString(1, firstName);
			pstmt.setString(2, lastName);
			pstmt.setString(3, sex);
			pstmt.setString(4, email);
			pstmt.setString(5, ssoid);
			pstmt.execute();
			pstmt.close();
			
			String user_map_role = "insert into BP_USER_ROLE(ssoid,collections)"
					+ " values(?,?)";
			PreparedStatement rpstmt = conn.prepareStatement(user_map_role);
			for(int i=0;i<ja.length();i++)
			{
				rpstmt.setString(1, ssoid);
				rpstmt.setString(2, ja.getString(i));
				rpstmt.execute();
				
			}
			rpstmt.close();
			
			
			conn.commit();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@POST
	@Path("/deleteuser")
	/*
	 * http://localhost:8080/businesspartner/rest/admin/deleteuser
	 	{
			"list":[
			
				{"SSOID":"user1@saptt.com"},
				{"SSOID":"user2@saptt.com"}
			
			]
		}
	 */
	public Response delete(String s)
	{
		JSONObject users = new JSONObject(s);
		JSONArray list = users.getJSONArray("list");
		boolean flag=true;
		for(int i=0;i<list.length();i++)
		{
			if(deleteUser(list.getJSONObject(i))==false)
				flag = false;
		}
		if(flag)
		{
			JSONObject res = new JSONObject();
			res.put("data", "success");
			Response response = Response.status(200).
	                entity(res.toString()).
	                header("Content-Type", "application/json; charset=utf-8").build();
			return response;
		}
		else
		{
			JSONObject res = new JSONObject();
			res.put("data", "failed");
			Response response = Response.status(400)
					.entity(res.toString()).header("Content-Type", 
							"application/json; charset=utf-8").build();
			return response;
		}		
	}
	
	private boolean deleteUser(JSONObject jo)
	{
		String ssoid = jo.getString("SSOID");
		String delMembSQL = "delete from BP_MEMBER where ssoid =?";
		String delMapSQL = "delete from BP_USER_ROLE where ssoid = ?";
		Connection conn = HANAConnection.getConnection();
		try {
			//foreign key
			//delete map first!
			PreparedStatement delMapStmt = conn.prepareStatement(delMapSQL);
			delMapStmt.setString(1, ssoid);
			delMapStmt.execute();
			delMapStmt.close();
			
			PreparedStatement delMemStmt = conn.prepareStatement(delMembSQL);
			delMemStmt.setString(1, ssoid);
			delMemStmt.execute();
			delMemStmt.close();
			
			conn.commit();
			conn.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	

	public static void main(String args[])
	{
		JSONObject jo = new JSONObject();
//		jo.put("SSOID","user6@saptt.com");
//		jo.put("FirstName", "Baoning");
//		jo.put("LastName", "Li");
//		jo.put("Email","gai@saptt.com");
//		jo.put("Sex","Male" );
		jo.put("SSOID","user4@saptt.com" );
		JSONArray ja = new JSONArray();
		ja.put(jo);
		JSONObject res = new JSONObject();
		res.put("list", ja);
		
//		JSONArray collections = new JSONArray();
//		collections.put("Transportation Planner");
//		collections.put("Outbound Delivery Manager");
//		collections.put("Inbound Supply Chain Planner");
//		jo.put("Collections", collections);
//		UserManagement test = new UserManagement();
//		boolean flag = test.deleteUser(jo);
		System.out.println(res.toString());
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
