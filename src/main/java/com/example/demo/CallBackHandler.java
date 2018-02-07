package com.example.demo;

import com.example.domain.SearchResult;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;
import com.github.messenger4j.MessengerPlatform;
import com.github.messenger4j.exceptions.MessengerApiException;
import com.github.messenger4j.exceptions.MessengerIOException;
import com.github.messenger4j.exceptions.MessengerVerificationException;
import com.github.messenger4j.receive.MessengerReceiveClient;
import com.github.messenger4j.receive.events.AccountLinkingEvent;
import com.github.messenger4j.receive.handlers.AccountLinkingEventHandler;
import com.github.messenger4j.receive.handlers.EchoMessageEventHandler;
import com.github.messenger4j.receive.handlers.FallbackEventHandler;
import com.github.messenger4j.receive.handlers.MessageDeliveredEventHandler;
import com.github.messenger4j.receive.handlers.MessageReadEventHandler;
import com.github.messenger4j.receive.handlers.OptInEventHandler;
import com.github.messenger4j.receive.handlers.PostbackEventHandler;
import com.github.messenger4j.receive.handlers.QuickReplyMessageEventHandler;
import com.github.messenger4j.receive.handlers.TextMessageEventHandler;
import com.github.messenger4j.send.MessengerSendClient;
import com.github.messenger4j.send.NotificationType;
import com.github.messenger4j.send.QuickReply;
import com.github.messenger4j.send.Recipient;
import com.github.messenger4j.send.SenderAction;
import com.github.messenger4j.send.buttons.Button;
import com.github.messenger4j.send.templates.GenericTemplate;

@RestController
@RequestMapping("/callback")
public class CallBackHandler {


	 private static final Logger logger = LoggerFactory.getLogger(CallBackHandler.class);

	private static final String RESOURCE_URL = "https://raw.githubusercontent.com/fbsamples/messenger-platform-samples/master/node/public";
	public static final String GOOD_ACTION = "DEVELOPER_DEFINED_PAYLOAD_FOR_GOOD_ACTION";
	public static final String NOT_GOOD_ACTION = "DEVELOPER_DEFINED_PAYLOAD_FOR_NOT_GOOD_ACTION";

	private final MessengerReceiveClient receiveClient;
	private final MessengerSendClient sendClient;

	@Autowired
	public CallBackHandler(@Value("${messenger4j.appSecret}") final String appSecret,
			@Value("${messenger4j.verifyToken}") final String verifyToken, final MessengerSendClient sendClient) {

		System.out.println("Initializing MessengerReceiveClient - appSecret: "+ appSecret +" verifyToken: "+ verifyToken);
		this.receiveClient = MessengerPlatform.newReceiveClientBuilder(appSecret, verifyToken)
				.onTextMessageEvent(newTextMessageEventHandler())
				.onQuickReplyMessageEvent(newQuickReplyMessageEventHandler()).onPostbackEvent(newPostbackEventHandler())
				.onAccountLinkingEvent(newAccountLinkingEventHandler()).onOptInEvent(newOptInEventHandler())
				.onEchoMessageEvent(newEchoMessageEventHandler())
				.onMessageDeliveredEvent(newMessageDeliveredEventHandler())
				.onMessageReadEvent(newMessageReadEventHandler()).fallbackEventHandler(newFallbackEventHandler())
				.build();
		this.sendClient = sendClient;
	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") final String mode,
			@RequestParam("hub.verify_token") final String verifyToken,
			@RequestParam("hub.challenge") final String challenge) {

		System.out.println("Received Webhook verification request - mode: "+ mode +" verifyToken: "+ verifyToken +" challenge: "+ challenge);
		try {
			return ResponseEntity.ok(this.receiveClient.verifyWebhook(mode, verifyToken, challenge));
		} catch (MessengerVerificationException e) {
			System.out.println("Webhook verification failed: "+ e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
	}

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Void> handleCallback(@RequestBody final String payload,
			@RequestHeader("X-Hub-Signature") final String signature) {

		System.out.println("Received Messenger Platform callback - payload: "+payload+" signature: " +signature);
		try {
			this.receiveClient.processCallbackPayload(payload, signature);
			System.out.println("Processed callback payload successfully ");
			return ResponseEntity.status(HttpStatus.OK).build();
		} catch (MessengerVerificationException e) {
			System.out.println("Processing of callback payload failed: "+ e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	private TextMessageEventHandler newTextMessageEventHandler() {
        return event -> {
            System.out.println("Received TextMessageEvent: "+ event);
            final String messageId = event.getMid();
            final String messageText = event.getText();
            final String senderId = event.getSender().getId();
            final Date timestamp = event.getTimestamp();
            System.out.println("Received message "+messageId+" with text "+messageText+" from user "+senderId+" at "+timestamp);
            try {
                switch (messageText.toLowerCase()) {


                    case "yo":
                        sendTextMessage(senderId, "Hello, What I can do for you ? Type the word you're looking for");
                        break;

                    case "great":
                        sendTextMessage(senderId, "You're welcome :) keep rocking");
                        break;
                    case "quick":
                    	sendQuickReply(senderId);
                    	break;
                    case "doc":
                    	sendSpringDoc(senderId, "spring");
                        break;
                    default:
                    	System.out.println("default response");
                    	sendTextMessage(senderId , "I can't understand ");
                        sendReadReceipt(senderId);
                    	sendTypingOn(senderId);
                       sendTypingOff(senderId);
                       break;
                }
            } catch (MessengerApiException e) {
				System.out.println("Messenger Api Exception 11111111111111  " + e);
				
			} catch (MessengerIOException e) {
				System.out.println("Messenger IO Exception 222222222222222 " + e);
			} catch (IOException e) {
				System.out.println("IO Exception 33 spring doc " + e);
			}
        };
    }

	private void sendSpringDoc(String recipientId, String keyword) throws IOException, MessengerApiException, MessengerIOException{

		Document doc = Jsoup.connect(("https://spring.io/search?q=").concat(keyword)).get();
		String countResult = doc.select("div.search-results--count").first().ownText();
		Elements searchResult = doc.select("section.search-result");
		List<SearchResult> searchResults = searchResult.stream()
				.map(element -> new SearchResult(element.select("a").first().ownText(),
						element.select("a").first().absUrl("href"),
						element.select("div.search-result--subtitle").first().ownText(),
						element.select("div.search-result--summary").first().ownText()))
				.limit(3).collect(Collectors.toList());

		final List<Button> firstLink = Button.newListBuilder().addUrlButton("Open Link", searchResults.get(0).getLink())
				.toList().build();
		final List<Button> secondLink = Button.newListBuilder()
				.addUrlButton("Open Link", searchResults.get(1).getLink()).toList().build();
		final List<Button> thirdtLink = Button.newListBuilder()
				.addUrlButton("Open Link", searchResults.get(2).getLink()).toList().build();
		final List<Button> searchLink = Button.newListBuilder()
				.addUrlButton("Open Link", ("https://spring.io/search?q=").concat(keyword)).toList().build();

		final GenericTemplate genericTemplate = GenericTemplate.newBuilder().addElements()
				.addElement(searchResults.get(0).getTitle()).subtitle(searchResults.get(0).getSubtitle())
				.itemUrl(searchResults.get(0).getLink())
				.imageUrl("https://upload.wikimedia.org/wikipedia/en/2/20/Pivotal_Java_Spring_Logo.png")
				.buttons(firstLink).toList().addElement(searchResults.get(1).getTitle())
				.subtitle(searchResults.get(1).getSubtitle()).itemUrl(searchResults.get(1).getLink())
				.imageUrl("https://upload.wikimedia.org/wikipedia/en/2/20/Pivotal_Java_Spring_Logo.png")
				.buttons(secondLink).toList().addElement(searchResults.get(2).getTitle())
				.subtitle(searchResults.get(2).getSubtitle()).itemUrl(searchResults.get(2).getLink())
				.imageUrl("https://upload.wikimedia.org/wikipedia/en/2/20/Pivotal_Java_Spring_Logo.png")
				.buttons(thirdtLink).toList().addElement("All results " + countResult).subtitle("Spring Search Result")
				.itemUrl(("https://spring.io/search?q=").concat(keyword))
				.imageUrl("https://upload.wikimedia.org/wikipedia/en/2/20/Pivotal_Java_Spring_Logo.png")
				.buttons(searchLink).toList().done().build();

		this.sendClient.sendTemplate(recipientId, genericTemplate);
	}

	private void sendGifMessage(String recipientId, String gif) throws MessengerApiException, MessengerIOException {
		this.sendClient.sendImageAttachment(recipientId, gif);
	}

	private void sendQuickReply(String recipientId) throws MessengerApiException, MessengerIOException {
		final List<QuickReply> quickReplies = QuickReply.newListBuilder().addTextQuickReply("Looks good", GOOD_ACTION)
				.toList().addTextQuickReply("Nope!", NOT_GOOD_ACTION).toList().build();

		this.sendClient.sendTextMessage(recipientId, "Was this helpful?!", quickReplies);
	}

	private void sendReadReceipt(String recipientId) throws MessengerApiException, MessengerIOException {
		this.sendClient.sendSenderAction(recipientId, SenderAction.MARK_SEEN);
	}

	private void sendTypingOn(String recipientId) throws MessengerApiException, MessengerIOException {
		this.sendClient.sendSenderAction(recipientId, SenderAction.TYPING_ON);
	}

	private void sendTypingOff(String recipientId) throws MessengerApiException, MessengerIOException {
		this.sendClient.sendSenderAction(recipientId, SenderAction.TYPING_OFF);
	}

	private QuickReplyMessageEventHandler newQuickReplyMessageEventHandler() {
		return event -> {
			System.out.println("Received QuickReplyMessageEvent: " + event);

			final String senderId = event.getSender().getId();
			final String messageId = event.getMid();
			final String quickReplyPayload = event.getQuickReply().getPayload();

			System.out.println("Received quick reply for message " + messageId + " with payload " + quickReplyPayload);

			try {
				if (quickReplyPayload.equals(GOOD_ACTION))
					sendGifMessage(senderId, "https://media.giphy.com/media/3oz8xPxTUeebQ8pL1e/giphy.gif");
				else
					sendGifMessage(senderId, "https://media.giphy.com/media/26ybx7nkZXtBkEYko/giphy.gif");
			} catch (MessengerApiException e) {
				handleSendException(e);
			} catch (MessengerIOException e) {
				handleIOException(e);
			}

			sendTextMessage(senderId, "Let's try another one :D!");
		};
	}

	private PostbackEventHandler newPostbackEventHandler() {
		return event -> {
			System.out.println("Received PostbackEvent: " + event);

			final String senderId = event.getSender().getId();
			final String recipientId = event.getRecipient().getId();
			final String payload = event.getPayload();
			final Date timestamp = event.getTimestamp();

			System.out.println("Received postback for user " + senderId + "  and page " + recipientId
					+ "  with payload  " + payload + " at " + timestamp);
			sendTextMessage(senderId, "Postback called");
		};
	}

	private AccountLinkingEventHandler newAccountLinkingEventHandler() {
		return event -> {
			System.out.println("Received AccountLinkingEvent: " + event);

			final String senderId = event.getSender().getId();
			final AccountLinkingEvent.AccountLinkingStatus accountLinkingStatus = event.getStatus();
			final String authorizationCode = event.getAuthorizationCode();

			System.out.println("account linking event for user " + senderId + "  with status " + accountLinkingStatus
					+ "  and auth code " + authorizationCode);
		};
	}

	private OptInEventHandler newOptInEventHandler() {
		return event -> {
			System.out.println("Received OptInEvent: " + event);

			final String senderId = event.getSender().getId();
			final String recipientId = event.getRecipient().getId();
			final String passThroughParam = event.getRef();
			final Date timestamp = event.getTimestamp();

			System.out.println("Received authentication for user " + senderId + "and page " + recipientId
					+ "  with pass through param " + passThroughParam + " at " + timestamp);

			sendTextMessage(senderId, "Authentication successful");
		};
	}

	private EchoMessageEventHandler newEchoMessageEventHandler() {
		return event -> {
			System.out.println("Received EchoMessageEvent: " + event);

			final String messageId = event.getMid();
			final String recipientId = event.getRecipient().getId();
			final String senderId = event.getSender().getId();
			final Date timestamp = event.getTimestamp();

			System.out.println("Received echo for message " + messageId + "that has been sent to recipient "
					+ recipientId + "  by sender " + senderId + " at " + timestamp);
		};
	}

	private MessageDeliveredEventHandler newMessageDeliveredEventHandler() {
		return event -> {
			System.out.println("Received MessageDeliveredEvent: " + event);

			final List<String> messageIds = event.getMids();
			final Date watermark = event.getWatermark();
			final String senderId = event.getSender().getId();

			if (messageIds != null) {
				messageIds.forEach(messageId -> {
					System.out.println("Received delivery confirmation for message : " + messageId);
				});
			}

			System.out.println("All messages before: " + watermark + " were delivered to user " + senderId);
		};
	}

	private MessageReadEventHandler newMessageReadEventHandler() {
		return event -> {
			System.out.println("Received MessageReadEvent " + event);

			final Date watermark = event.getWatermark();
			final String senderId = event.getSender().getId();

			System.out.println("All messages before:  " + watermark + " were read by user: " + senderId);
		};
	}

	private FallbackEventHandler newFallbackEventHandler() {
		return event -> {
			System.out.println("Received FallbackEvent: " + event);

			final String senderId = event.getSender().getId();
			System.out.println("Received unsupported message from user " + senderId);
		};
	}

	private void sendTextMessage(String recipientId, String text) {
		try {
			final Recipient recipient = Recipient.newBuilder().recipientId(recipientId).build();
			final NotificationType notificationType = NotificationType.REGULAR;
			final String metadata = "DEVELOPER_DEFINED_METADATA";

			this.sendClient.sendTextMessage(recipient, notificationType, text, metadata);
		} catch (MessengerApiException | MessengerIOException e) {
			handleSendException(e);
		}
	}

	private void handleSendException(Exception e) {
		System.out.println("Message could not be sent. An unexpected error occurred. " + e);
	}

	private void handleIOException(Exception e) {
		System.out.println("Could not open Spring.io page. An unexpected error occurred. " + e);
	}

}
