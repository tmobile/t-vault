// =========================================================================
// Copyright 2019 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================
package com.tmobile.cso.vault.api.bo;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.model.ADUserAccount;

import io.swagger.annotations.ApiModelProperty;
import org.springframework.util.StringUtils;

public class ADServiceAccount implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 6243675648100232920L;
	/**
	 * User id (First part of the email)
	 */
	private String userId;
	/**
	 * Email Id
	 */
	@JsonIgnore
	private String userEmail;
	/**
	 * Display Name
	 */
	private String displayName;
	/**
	 * Given Name
	 */
	private String givenName;
	
	/**
	 * User id or name
	 */
	private String userName;
	
	@JsonIgnore
	private Instant whenCreated;

	private String accountExpires;
    private String accountExpiresFormatted;

	private String pwdLastSet;
    private String pwdLastSetFormatted;

	private int maxPwdAge;
	
	private ADUserAccount managedBy;
	
	private String passwordExpiry;

	private String accountStatus;

	private String lockStatus;
	
	private String purpose;

	private String owner;

	private String memberOf;

	private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final TimeZone timeZonePST = TimeZone.getTimeZone(TVaultConstants.TIME_ZONE_PDT);

	/**
	 * @return the userId
	 */
	@ApiModelProperty(example="myfirstname.mylastname", position=1)
	public String getUserId() {
		return userId;
	}
	/**
	 * @return the userEmail
	 */
	@ApiModelProperty(example="myfirstname.mylastname@myorganization.com", position=2, hidden=true)
	public String getUserEmail() {
		return userEmail;
	}
	/**
	 * @return the displayName
	 */
	@ApiModelProperty(example="My First Name", position=3)
	public String getDisplayName() {
		return displayName;
	}
	/**
	 * @return the givenName
	 */
	@ApiModelProperty(example="My Last Name", position=4)
	public String getGivenName() {
		return givenName;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @param userEmail the userEmail to set
	 */
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	/**
	 * @param givenName the givenName to set
	 */
	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

    /**
     *
     * @param accountExpiresFormatted the accountExpiresFormatted to set
     */
    public void setAccountExpiresFormatted(String accountExpiresFormatted) {
        this.accountExpiresFormatted = accountExpiresFormatted;
    }

    /**
     *
     * @param pwdLastSetFormatted the pwdLastSetFormatted to set
     */
    public void setPwdLastSetFormatted(String pwdLastSetFormatted) {
        this.pwdLastSetFormatted = pwdLastSetFormatted;
    }
	/**
	 * @return the whenCreated
	 */
	@ApiModelProperty(hidden=true)
	public Instant getWhenCreated() {
		return whenCreated;
	}
	/**
	 * Formats the whenCreated using the pattern "yyyy-MM-dd HH:mm:ss"
	 * @return
	 */
	public String getCreationDate() throws IllegalArgumentException, DateTimeException {
		if (whenCreated != null) {
			return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(whenCreated, ZoneOffset.UTC));
		}
		return null;
	}
	/**
	 * @return the accountExpires
	 */
	public String getAccountExpires() {
		return accountExpires;
	}
    /**
     * @return the accountExpiresFormatted
     */
    public String getAccountExpiresFormatted() {
		/*
		   accountExpires can have values { 0, 9223372036854775807, valid Win32 filetime }, where the first 2 indicates account never expires
		   For a valid Win32 filetime, the date is calculated using the below calculation
		   Date accountExpires = new Date(accountExpiration/10000-TVaultConstants.FILETIME_EPOCH_DIFF); // FILETIME_EPOCH_DIFF:  Difference between Filetime epoch and Unix epoch = 11644473600000L
		 */
        String sAccountExpiration = TVaultConstants.NEVER_EXPIRE;
        try {
            dateFormat.setTimeZone(timeZonePST);
            long lAccountExpiration = Long.parseLong(accountExpires);
            // Check if account never expires
            if (TVaultConstants.SVC_ACC_NEVER_EXPIRE_VALUE != lAccountExpiration && lAccountExpiration != 0) {
                Date accountExpiresDate = new Date(lAccountExpiration/10000-TVaultConstants.FILETIME_EPOCH_DIFF);
                sAccountExpiration = dateFormat.format(accountExpiresDate);
            }
        }
        catch(Exception ex) {
            // Default TTL
            sAccountExpiration = TVaultConstants.NEVER_EXPIRE;
        }
        return accountExpiresFormatted = sAccountExpiration;
    }
	/**
	 * @return the passwordExpiry
	 */
	public String getPasswordExpiry() {
		/*
			passwordExpiry  = pwdLastSet + maxlife. If passwordExpiry  in past then password expired
		*/
		int maxLife = (int)TimeUnit.SECONDS.toDays(maxPwdAge);
		String pwdExpiryDateTime = TVaultConstants.EXPIRED;

		String memberOfStr = memberOf;
		if (!StringUtils.isEmpty(memberOfStr)) {
			Calendar c = Calendar.getInstance();
			if (!StringUtils.isEmpty(pwdLastSetFormatted)) {
				try{
					c.setTime(dateFormat.parse(pwdLastSetFormatted));
					c.add(Calendar.DAY_OF_MONTH, maxLife);
					if (c.getTime().before(new Date())) {
						pwdExpiryDateTime = TVaultConstants.EXPIRED;
					}
					else {
						String passwordExpiry = dateFormat.format(c.getTime());
						// find days to expire
						long difference = c.getTime().getTime() - new Date().getTime();
						String daysToExpire;
						if (difference >= TVaultConstants.DAY_IN_MILLISECONDS) { //more than one day
							daysToExpire = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS) + " days";
						}
						else { // less than one day
							daysToExpire = TimeUnit.HOURS.convert(difference, TimeUnit.MILLISECONDS) + " hours";
						}
						pwdExpiryDateTime = passwordExpiry +" ("+daysToExpire+")";
					}
				}catch(ParseException e){
					pwdExpiryDateTime = TVaultConstants.EMPTY;
				}
			}
		}
		return passwordExpiry = pwdExpiryDateTime;
	}
	/**
	 * @return the accountStatus
	 */
	public String getAccountStatus() {
		/*
			If accountExpires not in past, then accountStatus = active
		 */
		accountExpiresFormatted = getAccountExpiresFormatted();
		if (accountExpiresFormatted == null || accountExpiresFormatted.equals(TVaultConstants.NEVER_EXPIRE)) {
			accountStatus = "active";
		}
		else {
			try {
				dateFormat.setTimeZone(timeZonePST);
				accountStatus = "active";
				boolean expired = dateFormat.parse(accountExpiresFormatted).before(new Date());
				if (expired) {
					accountStatus = TVaultConstants.EXPIRED;
				}
			} catch (ParseException e) {
				accountStatus = "Unknown";
			}
		}
		return accountStatus;
	}
	/**
	 * @return the lockStatus
	 */
	public String getLockStatus() {
		return lockStatus;
	}
	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}
	/**
	 * @param whenCreated the whenCreated to set
	 */
	public void setWhenCreated(Instant whenCreated) {
		this.whenCreated = whenCreated;
	}
	/**
	 * @param accountExpires the accountExpires to set
	 */
	public void setAccountExpires(String accountExpires) {
		this.accountExpires = accountExpires;
	}
	/**
	 * @param passwordExpiry the passwordExpiry to set
	 */
	public void setPasswordExpiry(String passwordExpiry) {
		this.passwordExpiry = passwordExpiry;
	}
	/**
	 * @param accountStatus the accountStatus to set
	 */
	public void setAccountStatus(String accountStatus) {
		this.accountStatus = accountStatus;
	}
	/**
	 * @param lockStatus the lockStatus to set
	 */
	public void setLockStatus(String lockStatus) {
		this.lockStatus = lockStatus;
	}
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the pwdLastSet
	 */
	public String getPwdLastSet() {
		return pwdLastSet;
	}
    /**
     * @return the pwdLastSetFormatted
     */
    public String getPwdLastSetFormatted() {
    	/*
    		pwdLastSet uses the same calculation:
    		Date pwdSet = new Date(pwdLastSet/10000-TVaultConstants.FILETIME_EPOCH_DIFF);
    	 */
        String pwdLastSetDateTime = TVaultConstants.EMPTY;
        if (pwdLastSet!= null && !pwdLastSet.equals("0")) {
            try {
                dateFormat.setTimeZone(timeZonePST);
                long lpwdLastSetRaw = Long.parseLong(pwdLastSet);
                Date pwdSet = new Date(lpwdLastSetRaw/10000-TVaultConstants.FILETIME_EPOCH_DIFF);
                pwdLastSetDateTime = dateFormat.format(pwdSet);
            }
            catch(Exception ex) {
                pwdLastSetDateTime = TVaultConstants.EMPTY;
            }
        }
        return pwdLastSetFormatted = pwdLastSetDateTime;
    }
	/**
	 * @return the managedBy
	 */
	public ADUserAccount getManagedBy() {
		return managedBy;
	}
	/**
	 * @param pwdLastSet the pwdLastSet to set
	 */
	public void setPwdLastSet(String pwdLastSet) {
		this.pwdLastSet = pwdLastSet;
	}
	/**
	 * @param managedBy the managedBy to set
	 */
	public void setManagedBy(ADUserAccount managedBy) {
		this.managedBy = managedBy;
	}
	
	/**
	 * @return the maxPwdAge
	 */
	public int getMaxPwdAge() {
		int maxLife = 0;
		if (!StringUtils.isEmpty(memberOf)) {

			if (memberOf.contains(TVaultConstants.SVC_ACC_EXCEPTION)) {
				maxLife = TVaultConstants.SVC_ACC_EXCEPTION_MAXLIFE;
			} else {
				maxLife = TVaultConstants.SVC_ACC_STANDARD_MAXLIFE;
			}
		}
		return maxPwdAge = (int)TimeUnit.DAYS.toSeconds(maxLife);
	}
	/**
	 * @param maxPwdAge the maxPwdAge to set
	 */
	public void setMaxPwdAge(int maxPwdAge) {
		this.maxPwdAge = maxPwdAge;
	}
	public String getMemberOf() {
		return memberOf;
	}

	public void setMemberOf(String memberOf) {
		this.memberOf = memberOf;
	}

	/**
	 * @return the purpose
	 */
	public String getPurpose() {
		return purpose;
	}
	/**
	 * @param purpose the purpose to set
	 */
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	@Override
	public String toString() {
		return "ADServiceAccount [userId=" + userId + ", userEmail=" + userEmail + ", displayName=" + displayName
				+ ", givenName=" + givenName + ", userName=" + userName + ", whenCreated=" + whenCreated
				+ ", accountExpires=" + accountExpires + ", pwdLastSet=" + pwdLastSet + ", maxPwdAge=" + maxPwdAge
				+ ", managedBy=" + managedBy + ", passwordExpiry=" + passwordExpiry + ", accountStatus=" + accountStatus
				+ ", lockStatus=" + lockStatus + ", purpose=" + purpose + "]";
	}


	
}