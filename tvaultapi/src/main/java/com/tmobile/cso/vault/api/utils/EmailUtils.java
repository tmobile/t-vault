package com.tmobile.cso.vault.api.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.IAMServiceAccountConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.exception.LogMessage;

@Component
public class EmailUtils {

	@Autowired
	private JavaMailSender javaMailSender;

	@Autowired
	private TemplateEngine templateEngine;

	private Logger log = LogManager.getLogger(EmailUtils.class);
	
	public EmailUtils() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * To send HTML email notification
	 * @param from
	 * @param to
	 * @param subject
	 * @param variables
	 */
	public void sendHtmlEmalFromTemplate(String from, List<String> to, String subject, Map<String, String> variables) {
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = null;
		try {
			helper = new MimeMessageHelper(message,true, "UTF-8");
			helper.setFrom(from);
			helper.setTo(to.toArray(new String[to.size()]));
			helper.setSubject(subject);
			String templateFileName = TVaultConstants.EMAIL_TEMPLATE_NAME;

			// inline image content identifies
			for (Map.Entry<String, String> entry : TVaultConstants.EMAIL_TEMPLATE_IMAGE_IDS.entrySet()) {
				variables.put(entry.getKey(), entry.getKey());
			}
			String content = this.templateEngine.process(templateFileName, new Context(Locale.getDefault(), variables));
			helper.setText(content, true);
			try {
				// add each inline image byte scream
				for (Map.Entry<String, String> entry : TVaultConstants.EMAIL_TEMPLATE_IMAGE_IDS.entrySet()) {
					helper.addInline(entry.getKey(), getImageByteArray(entry.getValue()), TVaultConstants.IMAGE_TYPE_PNG);
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "sendHtmlEmalFromTemplate").
						put(LogMessage.MESSAGE, "Failed to get byte array from resource").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
			javaMailSender.send(message);
		} catch (MessagingException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendHtmlEmalFromTemplate").
					put(LogMessage.MESSAGE, String.format ("Failed to send email notification to Service account owner %s for service account %s", to.get(0), variables.get("svcAccName"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		} catch (MailSendException exception) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendHtmlEmalFromTemplate").
					put(LogMessage.MESSAGE, String.format ("Failed to send email notification to Service account owner %s for service account %s", to.get(0), variables.get("svcAccName"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
	}


	/**
	 * To send HTML email notification for internal certs
	 * @param from
	 * @param to
	 * @param subject
	 * @param variables
	 */
	public void sendHtmlEmalFromTemplateForInternalCert(String from, String to, String subject,
														Map<String, String> variables) {
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = null;
		try {
			helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject(subject);
			String templateFileName = TVaultConstants.EMAIL_TEMPLATE_NAME_CREATE_CERT;

			// inline image content identifies
			for (Map.Entry<String, String> entry : TVaultConstants.EMAIL_EXT_TEMPLATE_IMAGE_IDS.entrySet()) {
				variables.put(entry.getKey(), entry.getKey());
			}
			String content = this.templateEngine.process(templateFileName, new Context(Locale.getDefault(), variables));
			helper.setText(content, true);
			try {
				// add each inline image byte scream
				for (Map.Entry<String, String> entry : TVaultConstants.EMAIL_EXT_TEMPLATE_IMAGE_IDS.entrySet()) {
					helper.addInline(entry.getKey(), getImageByteArray(entry.getValue()), TVaultConstants.IMAGE_TYPE_PNG);
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "sendHtmlEmalFromTemplateForInternalCert").
						put(LogMessage.MESSAGE, "Failed to get byte array from resource").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
			javaMailSender.send(message);
		} catch (MessagingException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendHtmlEmalFromTemplateForInternalCert").
					put(LogMessage.MESSAGE, String.format("Failed to send email notification to  " +
							"User  %s for certificate %s for operation = %s", to, variables.get("certName"),
							variables.get("operation"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		} catch (MailSendException exception) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendHtmlEmalFromTemplateForInternalCert").
					put(LogMessage.MESSAGE, String.format("Failed to send email notification to  " +
									"User  %s for certificate %s for operation = %s", to, variables.get("certName"),
							variables.get("operation"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
	}

	/**
	 * To send HTML email notification for delete
	 * @param from
	 * @param to
	 * @param subject
	 * @param variables
	 */
	public void sendHtmlEmalFromTemplateForDelete(String from, String to, String subject,
														Map<String, String> variables) {
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = null;
		try {
			helper = new MimeMessageHelper(message,true, "UTF-8");
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject(subject);
			String templateFileName = TVaultConstants.EMAIL_TEMPLATE_NAME_DELETE_CERT;
			String content = this.templateEngine.process(templateFileName, new Context(Locale.getDefault(), variables));
			helper.setText(content, true);
			try {
				// add each inline image byte scream
				for (Map.Entry<String, String> entry : TVaultConstants.EMAIL_EXT_TEMPLATE_IMAGE_IDS.entrySet()) {
					helper.addInline(entry.getKey(), getImageByteArray(entry.getValue()), TVaultConstants.IMAGE_TYPE_PNG);
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "sendHtmlEmalFromTemplateForDelete").
						put(LogMessage.MESSAGE, "Failed to get byte array from resource").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
			javaMailSender.send(message);
		} catch (MessagingException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendHtmlEmalFromTemplateForDelete").
					put(LogMessage.MESSAGE, String.format("Failed to send email notification to  " +
									"User  %s for certificate %s for operation = %s", to, variables.get("certName"),
							variables.get("operation"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		} catch (MailSendException exception) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendHtmlEmalFromTemplateForDelete").
					put(LogMessage.MESSAGE, String.format("Failed to send email notification to  " +
									"User  %s for certificate %s for operation = %s", to, variables.get("certName"),
							variables.get("operation"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
	}



	/**
	 * To send HTML email notification for external cert(ceration and renewal)
	 * @param from
	 * @param to
	 * @param subject
	 * @param variables
	 */
	public void sendEmailForExternalCert(String from, String to, String subject,
															 Map<String, String> variables) {
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = null;
		try {
			helper = new MimeMessageHelper(message,true, "UTF-8");
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject(subject);
			String templateFileName = TVaultConstants.EMAIL_TEMPLATE_NAME_EXTERNAL_CERT;

			// inline image content identifies
			for (Map.Entry<String, String> entry : TVaultConstants.EMAIL_EXT_TEMPLATE_IMAGE_IDS.entrySet()) {
				variables.put(entry.getKey(), entry.getKey());
			}
			String content = this.templateEngine.process(templateFileName, new Context(Locale.getDefault(), variables));
			helper.setText(content, true);
			try {
				// add each inline image byte scream
				for (Map.Entry<String, String> entry : TVaultConstants.EMAIL_EXT_TEMPLATE_IMAGE_IDS.entrySet()) {
					helper.addInline(entry.getKey(), getImageByteArray(entry.getValue()), TVaultConstants.IMAGE_TYPE_PNG);
				}
			} catch (IOException e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "sendEmailForExternalCert").
						put(LogMessage.MESSAGE, "Failed to get byte array from resource").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
			}
			javaMailSender.send(message);
		} catch (MessagingException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendEmailForExternalCert").
					put(LogMessage.MESSAGE, String.format("Failed to send email notification to  " +
									"User  %s for certificate %s for operation = %s", to, variables.get("certName"),
							variables.get("operation"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		} catch (MailSendException exception) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendEmailForExternalCert").
					put(LogMessage.MESSAGE, String.format("Failed to send email notification to  " +
									"User  %s for certificate %s for operation = %s", to, variables.get("certName"),
							variables.get("operation"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
	}

	/**
	 * To send HTML email notification for delete
	 *
	 * @param from
	 * @param to
	 * @param subject
	 * @param variables
	 */
	public void sendTransferEmail(String from, Map<String, String> variables, String subject) {
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = null;
		try {
			helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(from);
			helper.setTo(variables.get("newOwnerEmail"));
			helper.setCc(variables.get("oldOwnerEmail"));
			helper.setSubject(subject);
			String templateFileName = TVaultConstants.EMAIL_TEMPLATE_NAME_TRANSFER;
			String content = this.templateEngine.process(templateFileName, new Context(Locale.getDefault(), variables));
			helper.setText(content, true);
			javaMailSender.send(message);
		} catch (MessagingException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendTransferEmail").
					put(LogMessage.MESSAGE, String.format("Failed to send Transfer email notification to  " +
									"User  %s for certificate %s for operation = %s", variables.get("newOwnerEmail"), variables.get("certName"),
							variables.get("operation"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		} catch (MailSendException exception) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "sendTransferEmail").
					put(LogMessage.MESSAGE, String.format("Failed to send Transfer email notification to  " +
									"User  %s for certificate %s for operation = %s", variables.get("newOwnerEmail"), variables.get("certName"),
							variables.get("operation"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
	}
	/**
	 * To get byte array stream of image resources
	 * @param imagePath
	 * @return
	 * @throws IOException
	 */

	private ByteArrayResource getImageByteArray(String imagePath) throws IOException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream(imagePath);
		BufferedImage bImage = ImageIO.read(is);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(bImage, TVaultConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream);
		return new ByteArrayResource(byteArrayOutputStream.toByteArray());
	}

	/**
	 * To send HTML email notification
	 * @param from
	 * @param to
	 * @param subject
	 * @param variables
	 */
	public void sendIAMSvcAccHtmlEmalFromTemplate(String from, List<String> to, String subject, Map<String, String> variables) {
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = null;
		try {
			helper = new MimeMessageHelper(message,true, "UTF-8");
			helper.setFrom(from);
			helper.setTo(to.toArray(new String[to.size()]));
			helper.setSubject(subject);
			String templateFileName = IAMServiceAccountConstants.IAM_EMAIL_TEMPLATE_NAME;

			// inline image content identifies
			for (Map.Entry<String, String> entry : IAMServiceAccountConstants.IAM_EMAIL_TEMPLATE_IMAGE_IDS.entrySet()) {
				variables.put(entry.getKey(), entry.getKey());
			}
			String content = this.templateEngine.process(templateFileName, new Context(Locale.getDefault(), variables));
			helper.setText(content, true);
			extractImageBytesFromByteArray(helper);
			javaMailSender.send(message);
		} catch (MessagingException|MailSendException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "sendIAMSvcAccHtmlEmalFromTemplate").
					put(LogMessage.MESSAGE, String.format ("Failed to send email notification to IAM Service account owner %s for IAM service account %s", to.get(0), variables.get("svcAccName"))).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
	}

	/**
	 * @param helper
	 * @throws MessagingException
	 */
	private void extractImageBytesFromByteArray(MimeMessageHelper helper) throws MessagingException {
		try {
			// add each inline image byte scream
			for (Map.Entry<String, String> entry : TVaultConstants.EMAIL_TEMPLATE_IMAGE_IDS.entrySet()) {
				helper.addInline(entry.getKey(), getImageByteArray(entry.getValue()), TVaultConstants.IMAGE_TYPE_PNG);
			}
		} catch (IOException e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "sendIAMSvcAccHtmlEmalFromTemplate").
					put(LogMessage.MESSAGE, "Failed to get byte array from resource").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
	}
}
