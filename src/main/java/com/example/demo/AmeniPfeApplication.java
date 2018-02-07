package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import com.github.messenger4j.MessengerPlatform;
import com.github.messenger4j.send.MessengerSendClient;


@SpringBootApplication
public class AmeniPfeApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(AmeniPfeApplication.class);

	@Bean
	public MessengerSendClient messengerSendClient(@Value("${messenger4j.pageAccessToken}") String pageAccessToken) {
		logger.debug("Initializing MessengerSendClient - pageAccessToken: {}", pageAccessToken);
		System.out.println("page Access Token" + pageAccessToken );
		return MessengerPlatform.newSendClientBuilder(pageAccessToken).build();
}
	
	public static void main(String[] args) {
		SpringApplication.run(AmeniPfeApplication.class, args);
	}
}
