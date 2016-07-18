package Mails;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import invitation.InvitationBP;

public class InvitationMail {
	private static Properties prop = new Properties();
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
	}
	
	public static boolean sendMail(String mail,String inviteeFirstName,String inviteeLastName,String inviterName,String bpName)
	{
		boolean flag = false;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String inviteURL = prop.getProperty("INVITE_MAIL_URL"); 
		HttpPost mailPost = new HttpPost(inviteURL);
		mailPost.setHeader("Content-Type","application/json");
		String inviteAuthorization = prop.getProperty("INVITE_MAIL_AUTHORIZATION");
		mailPost.setHeader("Authorization",inviteAuthorization);

        JSONObject jo =  new JSONObject();
        jo.put("inviteeEmail", mail);
        jo.put("inviteeFirstName", inviteeFirstName);
        jo.put("inviteeLastName", inviteeLastName);
        jo.put("inviterName", inviterName);
        jo.put("headerText", prop.getProperty("MAIL_HEADERTEXT"));
        jo.put("footerText", prop.getProperty("MAIL_FOOTERTEXT"));
        jo.put("targetUrl",prop.getProperty("MAIL_ACCEPT_URL").replace("$BPNAME$", bpName));
      //  System.out.println("Json Body: " + jsonBody.toString());
        StringEntity requestEntity = new StringEntity(
                jo.toString(),
                ContentType.APPLICATION_JSON);
		
        mailPost.setEntity(requestEntity);
        try {
			CloseableHttpResponse  response = httpClient.execute(mailPost);
            HttpEntity entity = response.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity);
            response.close();
            httpClient.close();
            flag = true;
			
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			return flag;
        }
	}
}
