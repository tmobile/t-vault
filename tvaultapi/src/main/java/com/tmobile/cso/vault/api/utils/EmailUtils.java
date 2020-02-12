package com.tmobile.cso.vault.api.utils;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailUtils {

	@Autowired
	private JavaMailSender javaMailSender;
	
	private Logger log = LogManager.getLogger(EmailUtils.class);
	
	public EmailUtils() {
		// TODO Auto-generated constructor stub
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @param subject
	 * @param mailBody
	 */
	public void sendPlainTextEmail(String from, List<String> to, String subject, String mailBody) {
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "sendPlainTextEmail").
				put(LogMessage.MESSAGE, String.format ("Sending email notification to Service account owner on successful onboarding.")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to.toArray(new String[to.size()]));
        msg.setSubject(subject);
        msg.setText(mailBody);
        try {
			javaMailSender.send(msg);
		}catch (MailException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendPlainTextEmail").
					put(LogMessage.MESSAGE, String.format ("Failed to send email notification to Service account owner.")).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
    }
}
