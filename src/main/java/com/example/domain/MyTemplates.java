package com.example.domain;

import java.util.List;

import com.github.messenger4j.exceptions.MessengerApiException;
import com.github.messenger4j.exceptions.MessengerIOException;
import com.github.messenger4j.send.QuickReply;

public class MyTemplates {
	public static final String FIRST_JOB = "job_1";
	public static final String SECOND_JOB = "job_2";
	public static final String THIRD_JOB = "job_3";
	
	
	public MyTemplates() {
		
	}
	
	public List<QuickReply>  myButtonTest() throws MessengerApiException, MessengerIOException {
		final List<QuickReply> myOptions = QuickReply.newListBuilder()
				.addTextQuickReply("First job", FIRST_JOB).toList()
				.addTextQuickReply("Second job", SECOND_JOB).toList()
				.addTextQuickReply("Third Job", THIRD_JOB).toList()
				.build();
		
		return myOptions;
		//this.sendClient.sendTextMessage(recipientId, "You have to choose a Job", myOptions);

	}
}
