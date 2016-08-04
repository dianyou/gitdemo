package invitation;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 * select business partner
 * @author Gavin 
 * 
 */
@Path("bplist")
public class BPList {
	private static Properties prop = new Properties();
	private static String DB_bpSchema; 
	private static String DB_bpDir; 

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
		DB_bpSchema = prop.getProperty("HANA_SCHEMA_BP");
		DB_bpDir = prop.getProperty("HANA_BP_TABLE");
	}
	
	/**
	 * http://localhost:8080/businesspartner/rest/bplist/members
	 * 两次的list页面全都使用这个url
	 */
	@GET
	@Path("/members")
	@Produces(MediaType.APPLICATION_JSON)
	public Response invitationList()
	{
		String s = getinvitationList(null);
		//to unicode
		Response res = Response.status(200).
                entity(s).
                header("Content-Type", "application/json; charset=utf-8").build();
		return res;
	}
	

	/**
	 * http://localhost:8080/businesspartner/rest/bplist/search?name=ibm
	 * @param name
	 * @return
	 * for now, search with front end
	 */
	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response search(@QueryParam("name") String name)
	{
		String s = getinvitationList(name);
		//to unicode
		Response res = Response.status(200).
                entity(s).
                header("Content-Type", "application/json; charset=utf-8").build();
		return res;
	}
	
	private String getinvitationList(String name)
	{
		JSONArray ja = new JSONArray();
		JSONObject outline = new JSONObject();
		
		//connect to HANA
		Connection con = HANAConnection.getConnection();
		if(con==null)
		{
			outline.put("list", ja);
			return outline.toString();
		}		
		String sql =null;
		if(name ==null)
		{
			sql= prop.getProperty("SQL_BPLIST");
		}
		else
		{
			sql= new StringBuilder("select * from ").append(DB_bpDir).append(" where UPPER(BPNAME)='")
					.append(name.toUpperCase()).append("' order by STATUS desc,BPNAME asc").toString();
		}
		ResultSet rs = null;
//		System.out.println(sql.toString());
		try {
			Statement stmt= con.createStatement();
			//update the pending status to expired!
			String updateSQL = prop.getProperty("SQL_EXPIRED_UPDATE");
			//System.out.println(updateSQL);
		
			stmt.execute(updateSQL);
			con.commit();
			
			rs = stmt.executeQuery(sql);
			while(rs != null &&rs.next())
			{
				JSONObject jo = new JSONObject();
				//bp name
				String bpName = rs.getNString("BPNAME");
				String status = rs.getNString("STATUS");
				//bp date format:yyyy-MM-DD HI24:mm:ss
				String dateType=null;
				if(status.toUpperCase().equals("JOINED"))
					dateType = "JOINEDDATE";
				else if(status.toUpperCase().equals("PENDING"))
					dateType = "INVITATIONDATE";
				else if(status.toUpperCase().equals("EXPIRED"))
					dateType = "EXPIREDDATE"; 
				if(dateType!=null)
				{
					String bpDate = rs.getNString(dateType);
					if(bpDate !=null)
						bpDate = bpDate.substring(0, bpDate.lastIndexOf("."));
					jo.put("date", bpDate);
				}
				jo.put("name", bpName);
				jo.put("status", status);
				jo.put("info",rs.getNString("BPINFO"));
				ja.put(jo);
			}
			
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   
		outline = new JSONObject();
		outline.put("list", ja);
		return outline.toString();
	}
}
