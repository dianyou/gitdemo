package invitation;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.json.JSONArray;

import Mails.InvitationMail;

public class SendMailThreads implements Runnable{
	private static Properties prop = new Properties();
	String mail;
	String inviteeName;
	String inviterName;
	JSONArray invitedMembers;
	JSONArray emailSentFailed;
	
	
	public SendMailThreads(String mail,String inviteeName,String inviterName,
			JSONArray invitedMembers,JSONArray emailSentFailed)
	{
		this.mail = mail;
		this.inviteeName = inviteeName;
		this.inviterName = inviterName;
		this.invitedMembers = invitedMembers;
		this.emailSentFailed = emailSentFailed;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		boolean isSent = InvitationMail.sendMail(mail, inviteeName, inviterName);
		if(isSent)
			invitedMembers.put(inviteeName);
		else
			emailSentFailed.put(inviteeName);
		
	}

}
