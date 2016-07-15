package unused;


import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import hana.HANAConnection;

/**
 * page 1 of mock up
 * @author Gavin
 *	暂时不使用。。。。。。。。
 */
@Path("outline")
public class BPOutline {
	
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
	
	/**
	 * http://10.131.6.232:8089/gavin/bp/outline/list?status=pending
	 * @param status
	 * @return
	 */
	@GET
	@Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response outline(@QueryParam("status") String status)
	{
		String s = getList(status,null);
		//to unicode
		Response res = Response.status(200).
                entity(s).
                header("Content-Type", "application/json; charset=utf-8").build();
		return res;
	}
	
	/**
	 * http://localhost:8080/gavin/bp/outline/search?status=PENDING&name=Coca-Cola
	 * @param status
	 * @param name
	 * @return
	 */
	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response search(@QueryParam("status") String status,
				@QueryParam("name") String name)
	{
		String s = getList(status,name);
		//to unicode
		Response res = Response.status(200).
                entity(s).
                header("Content-Type", "application/json; charset=utf-8").build();
		return res;
	}
	
	private String getList(String status,String name)
	{
		JSONArray ja = new JSONArray();
		JSONObject outline = new JSONObject();
		
		String bpSchema = prop.getProperty("HANA_SCHEMA_BP");
		//connect to HANA
		
		Connection con = HANAConnection.getConnection();
		if(con==null)
		{
			outline.put("list", ja);
			return outline.toString();
		}
		//判断获取哪个日期
		String dateType;
		if(status.toUpperCase().equals("JOINED"))
			dateType = "JOINEDDATE";
		else if(status.toUpperCase().equals("REJECTED"))
			dateType = "REJECTEDDATE";
		else if(status.toUpperCase().equals("PENDING"))
			dateType = "INVITATIONDATE";
		else
		{
			outline.put("list", ja);
			return outline.toString();
		}
		
		StringBuilder sql =null;
		if(name == null)
		{
			 sql= new StringBuilder("select * from ").append(bpSchema).
						append(".").append("BPDIRECTORY").append(" where status='").
						append(status.toUpperCase()).append("'");
		}
		else
		{
			 sql= new StringBuilder("select * from ").append(bpSchema).
						append(".").append("BPDIRECTORY").append(" where STATUS='").
						append(status.toUpperCase()).append("' and UPPER(BPNAME)=\'").append(name.toUpperCase())
						.append("'");
		}
		ResultSet rs = null;
		try {
			Statement stmt= con.createStatement();
			rs = stmt.executeQuery(sql.toString());
			while(rs != null &&rs.next())
			{
				//bp name
				String bpName = rs.getNString("BPNAME");
				//bp date format:yyyy-MM-DD HI24:mm:ss
				String bpDate = rs.getNString(dateType);
				if(bpDate !=null)
					bpDate = bpDate.substring(0, bpDate.lastIndexOf("."));
				JSONObject jo = new JSONObject();
				jo.put("name", bpName);
				jo.put("date", bpDate);
				jo.put("status", rs.getNString("STATUS"));
				ja.put(jo);
			}
			
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   
      //  System.out.println("1###" + jArr.toString());
		outline = new JSONObject();
		outline.put("list", ja);
		return outline.toString();
	}
	


}
