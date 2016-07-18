

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.auth.UsernamePasswordCredentials;
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

public class Memo {
	private static Properties prop = new Properties();
	private static String DB_bpSchema;
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
	}
	private void invite(String s)
	{
		String bpSchema = prop.getProperty("HANA_BP_SCHEMA");
		JSONObject jo = new JSONObject(s);
		JSONArray ja = jo.getJSONArray("members");
		String period = jo.getString("period");
		System.out.println(period);
		Connection con = HANAConnection.getConnection();
		if(con==null)
		{
			return;
		}	
		for(int i=0;i<ja.length();i++)
		{
			String bpName = ja.getString(i);
			System.out.println(bpName);
			String isInvitedSQL = new StringBuilder("select * from ").append(bpSchema).
					append(".").append("BPDIRECTORY").append(" where UPPER(BPNAME)='")
					.append(bpName.toUpperCase()).append("'").toString();
			System.out.println(isInvitedSQL.toString());
			ResultSet rs = null;
			String email=null;

			try
			{
				Statement stmt= con.createStatement();
				rs = stmt.executeQuery(isInvitedSQL);
				if(rs.next())
				{
					String status = rs.getNString("STATUS");
					System.out.println("Status::"+status);
					if(status.toUpperCase().equals("JOINED"))
					{
						//has joined
							continue;
					}
					else if(status.toUpperCase().equals("PENDING"))
					{
						//has invited,but still not get the answer
						continue;
					}
					email = rs.getNString("EMAIL");

					String updateSQL = new StringBuilder("update ")
						.append(bpSchema).append(".").append("BPDIRECTORY")
							.append(" set STATUS='PENDING',INVITATIONDATE=CURRENT_TIMESTAMP")
							.append(",INVITATIONVALID=").append(period)
							.append(" where UPPER(BPNAME)='")
							.append(bpName.toUpperCase()).append("'")
							.toString();
					System.out.println(updateSQL);

					String insertSQL = new StringBuilder("insert into ")
						.append(bpSchema).append(".").append("INVITATIONLIST")
						.append("(BPNAME,EMAIL,INVITATIONDATE,VALID,STATUS)")
						.append("values('").append(bpName).append("','").append(email)
						.append("',").append("CURRENT_TIMESTAMP,").append(period)
						.append(",'PENDING')").toString();
					System.out.println(insertSQL);

					stmt.execute(updateSQL);
					stmt.execute(insertSQL);
					stmt.close();
				//	con.commit();
				

				}
				else
				 	return;
				
			}
			catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

			if(email==null||email.length()==0)
				continue;
			//sendMail(email);
			
		}
		try {
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	private void sendMail(String email,String inviteeName,String inviterName)
	{
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String inviteURL = prop.getProperty("INVITE_MAIL_URL"); 
		HttpPost mailPost = new HttpPost(inviteURL);
		mailPost.setHeader("Content-Type","application/json");
		String inviteAuthorization = prop.getProperty("INVITE_MAIL_AUTHORIZATION");
		mailPost.setHeader("Authorization",inviteAuthorization);

        JSONObject jo =  new JSONObject();
        jo.put("inviteeEmail", email);
        jo.put("inviteeFirstName", inviteeName);
        //jo.put("inviteeLastName", "Gai");
        jo.put("inviterName", inviterName);
        jo.put("headerText", prop.getProperty("MAIL_HEADERTEXT"));
        jo.put("footerText", prop.getProperty("MAIL_FOOTERTEXT"));
     //   jo.put("targetUrl",prop.getProperty("MAIL_ACCEPT_URL"));
        jo.put("targetUrl","http://www.baidu.com");
        //jo.put("sourceUrl", "http://www.baidu.com");
        //  System.out.println("Json Body: " + jsonBody.toString());
        StringEntity requestEntity = new StringEntity(
                jo.toString(),
                ContentType.APPLICATION_JSON);
		
        mailPost.setEntity(requestEntity);
        System.out.println(mailPost.getAllHeaders()[0] + ";" + mailPost.getAllHeaders()[1] );
        try {
			CloseableHttpResponse  response = httpClient.execute(mailPost);
            System.out.println("Response Code : "
                    + response.getStatusLine().getStatusCode());
            System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity);
            response.close();
            httpClient.close();
			
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			
        }
	
	}
	
	private String mergeHanaSCI()
	{
		JSONArray ja = new JSONArray();
		JSONObject outline = new JSONObject();
		
		//get users from SCI
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String userlistURL = prop.getProperty("SCI_USERLIST_URL"); 
		String userlistAuthorization = prop.getProperty("SCI_USERLIST_AUTHORIZATION");
		
		
		
		//connect to HANA
		Connection con = HANAConnection.getConnection();
		if(con==null)
		{
			outline.put("list", ja);
			return outline.toString();
		}		
		StringBuilder sql =null;
		sql= new StringBuilder("select * from ").append(DB_bpSchema).
					append(".").append("BPDIRECTORY").append(" order by STATUS desc,BPNAME asc");
		ResultSet rs = null;
		try {
			Statement stmt= con.createStatement();
			rs = stmt.executeQuery(sql.toString());
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
				else if(status.toUpperCase().equals("REJECTED"))
					dateType = "REJECTEDDATE";
				else if(status.toUpperCase().equals("PENDING"))
					dateType = "INVITATIONDATE";
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

	
	public static void main(String args[])
	{
		String s = "{\"members\":[\"IBM\",\"APPLE\","
				+"\"American Express\"],\"period\":\"24\"}";
		System.out.println(s);
//		String path = 
//				Memo.class.getClassLoader()
//				.getResource("hanaInfo.properties").getFile();
//		System.out.println(path);
		Memo test = new Memo();
		
		String sql = prop.getProperty("SQL_BPLIST");
		System.out.println(sql);

	}

}
