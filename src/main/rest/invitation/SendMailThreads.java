package invitation;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.json.JSONArray;

import Mails.InvitationMail;

public class SendMailThreads implements Runnable{
	private static Properties prop = new Properties();
	String mail;
	String inviteeFirstName;
	String inviteeLastName;
	String inviterName;
	JSONArray invitedMembers;
	JSONArray emailSentFailed;
	String bpName;
	
	
	public SendMailThreads(String mail,String inviteeFirstName,String inviteeLastName,String inviterName,
			JSONArray invitedMembers,JSONArray emailSentFailed,String bpName)
	{
		this.mail = mail;
		this.inviteeFirstName = inviteeFirstName;
		this.inviteeLastName = inviteeLastName;
		
		this.inviterName = inviterName;
		this.invitedMembers = invitedMembers;
		this.emailSentFailed = emailSentFailed;
		this.bpName = bpName;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		boolean isSent = InvitationMail.sendMail(mail, inviteeFirstName,inviteeLastName, inviterName,bpName);
		if(isSent)
			invitedMembers.put(bpName);
		else
			emailSentFailed.put(bpName);
		
	}

}
