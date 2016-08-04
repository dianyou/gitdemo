package invitation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import Mails.InvitationMail;

public class InvitationBPTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testInvite() {
		boolean flag = 
				InvitationMail.sendMail("gg8912@gmail.com","Gai" ,"Wen", "SAP","NIKE");
	//	flag = ibp.sendMail("w19891212sdfasdfadsfsdaf@163.com", "haha文", "me");
		//System.out.println(flag);
		assertEquals(true,flag);
	//	fail("Not yet implemented");
	}

}
