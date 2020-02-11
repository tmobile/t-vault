package com.tmobile.cso.vault.api.utils;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
	 * @param subjectst0
	 * @param mailBody
	 */
	public void sendPlainTextEmail(String from, List<String> to, String subject, String mailBody) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to.toArray(new String[to.size()]));
        msg.setSubject(subject);
        msg.setText(mailBody);
        javaMailSender.send(msg);
    }
}
