package invitation;


import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import hana.HANAConnection;

/**
 * select business partner
 * @author Gavin 
 * 
 */
@Path("invitation")
public class InvitationBP {
	private static Properties prop = new Properties();
	private static String DB_bpSchema; 
	private static String DB_bpDir; 

	static
	{
		//get path for properties
		String path = InvitationBP.class.getClassLoader()
				.getResource("hanaInfo.properties").getFile();
		try {
			prop.load(new FileInputStream(path.toString()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DB_bpSchema = prop.getProperty("HANA_SCHEMA_BP");
		DB_bpDir = prop.getProperty("HANA_BP_TABLE");
	}
	
	@POST
	@Path("/invite")
	//http://localhost:8080/businesspartner/rest/invitation/invite
//	@Consumes(MediaType.APPLICATION_ATOM_XML)
//	@Produces(MediaType.TEXT_PLAIN)  
	public Response invite(String s)
	{
		JSONObject jo = sendInvitation(s);
		Response res = Response.status(200).
                entity(jo.toString()).
                header("Content-Type", "application/json; charset=utf-8").build();
		return res;
	}
	
	@POST
	@Path("/feedback")
	//http://localhost:60230/SolBP/#/register?bp=1
	public Response feedback(String s)
	{
		JSONObject jo = new JSONObject(s);
		String bpName = jo.getString("name").replace("%20", " ")//去掉空格
				.toUpperCase();
		JSONObject res = new JSONObject();
		Response response=null;
		Connection con = HANAConnection.getConnection();
		if(con==null)
		{
			String error= "Cannot connect to HANA DB";
			res.put("Error:",error);
			res.put("Updated", false);
			return Response.status(200).
	                entity(res.toString()).
	                header("Content-Type", "application/json; charset=utf-8").build();
		}
		String updateSQL = prop.getProperty("SQL_FEEDBACK")
				.replace("$BPNAME$", bpName);
		
		try {
			Statement  stmt= con.createStatement();
	//		System.out.println(updateSQL);
			stmt.execute(updateSQL);
			con.commit();
			res.put("updated",true );
			
			stmt.close();
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.status(200).
                entity(res.toString()).
                header("Content-Type", "application/json; charset=utf-8").build();

	}
	
	private JSONObject sendInvitation(String s)
	{

		JSONObject jo = new JSONObject(s);
		JSONObject res = new JSONObject();
		JSONArray ja = jo.getJSONArray("members");
	
		Connection con = HANAConnection.getConnection();
		JSONArray joinedMembers = new JSONArray();
		JSONArray pendingMembers = new JSONArray();
		JSONArray invitedMembers = new JSONArray();
		JSONArray noValidEmailMemebers = new JSONArray();
		JSONArray emailSentFailed = new JSONArray();
		
		int cpuNums = Runtime.getRuntime().availableProcessors();
		ExecutorService pool = Executors.newFixedThreadPool(cpuNums);
		
		if(con==null)
		{
			String error= "Cannot connect to HANA DB";
			res.put("Error",error );
			return res;
		}	
		/**
		First, it needs to judge whether the status of members is correct!
		*/
		for(int i=0;i<ja.length();i++)
		{
			String bpName = ja.getString(i);
	//		System.out.println(bpName);
			String isInvitedSQL = new StringBuilder("select * from ").append(DB_bpSchema).
					append(".").append("BPDIRECTORY").append(" where UPPER(BPNAME)='")
					.append(bpName.toUpperCase()).append("'").toString();
			
	//		System.out.println(isInvitedSQL.toString());
			ResultSet rs = null;
			String email=null;
			String period=prop.getProperty("VALID_PERIOD");

			try
			{
				Statement stmt= con.createStatement();
				rs = stmt.executeQuery(isInvitedSQL);
				if(rs.next())
				{
					String status = rs.getNString("STATUS");
//					System.out.println("Status::"+status);
					if(status.toUpperCase().equals("JOINED"))
					{
						//has joined
						joinedMembers.put(bpName);
						continue;
					}
					else if(status.toUpperCase().equals("PENDING"))
					{
						//has invited,but still not get the answer
						pendingMembers.put(bpName);
						continue;
					}
				
					

					//get bp members
					String getMemberSQL = new StringBuilder("select * from ").append(DB_bpSchema).
							append(".").append("BP_MEMBER").append(" where UPPER(BP)='")
							.append(bpName.toUpperCase()).append("'")
							.append(" and PRIMARIED=1")
							.toString();
//					System.out.println(getMemberSQL);
					ResultSet mrs = stmt.executeQuery(getMemberSQL);
					String inviteeFirstName=null;
					String inviteeLastName = null;
					if(mrs.next())
					{
						email = mrs.getNString("EMAIL");
						inviteeFirstName = mrs.getNString("FIRSTNAME");
						inviteeLastName = mrs.getNString("LASTNAME");
					}
					
					if(email==null||email.length()==0)
					{
						noValidEmailMemebers.put(bpName);
						continue;
					}
					
						
					String updateSQL = new StringBuilder("update ")
						.append(DB_bpSchema).append(".").append("BPDIRECTORY")
							.append(" set STATUS='PENDING',INVITATIONDATE=CURRENT_TIMESTAMP,")
							.append("INVITATIONVALID=").append(period)
							.append(" where UPPER(BPNAME)='")
							.append(bpName.toUpperCase()).append("'")
							.toString(); 
	//				System.out.println(updateSQL);

					String insertSQL = new StringBuilder("insert into ")
						.append(DB_bpSchema).append(".").append("INVITATIONLIST")
						.append("(BPNAME,EMAIL,INVITATIONDATE,VALID,STATUS)")
						.append("values('").append(bpName).append("','").append(email)
						.append("',").append("CURRENT_TIMESTAMP,").append(period)
						.append(",'PENDING')").toString();
	//				System.out.println(insertSQL);
					
					if(inviteeFirstName==null )
					{
						if(inviteeLastName==null)
						{
							inviteeFirstName=bpName;
							inviteeLastName="";
						}
					}
					else if(inviteeLastName==null)
					{
						inviteeLastName="";
					}

					SendMailThreads sendThread = new SendMailThreads(email, inviteeFirstName,inviteeLastName,
							"SAP",invitedMembers,emailSentFailed,bpName);
					pool.execute(sendThread);
					stmt.execute(updateSQL);
					stmt.execute(insertSQL);
					stmt.close();
					con.commit();
				}
				else
				 	return res;
				
			}
			catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

		}
		
		try {
			pool.shutdown();
			if(pool.awaitTermination(180,TimeUnit.SECONDS))
				System.out.println("All threads finished!");
			else
				System.out.println("Not all threads finished!");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		res.put("Joined",joinedMembers); 
		res.put("Pending", pendingMembers);//had invited
		res.put("Invited", invitedMembers); //invite succefully this time
		res.put("EmailSentFailed", emailSentFailed);
		res.put("NotValidEmail", noValidEmailMemebers);
		return res;
	}
	
}
