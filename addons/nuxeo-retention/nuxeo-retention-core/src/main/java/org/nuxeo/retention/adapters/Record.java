/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Guillaume RENARD
 */
package org.nuxeo.retention.adapters;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.retention.RetentionConstants;

/**
 * @since 11.1
 */
public class Record {

    private static final Logger log = LogManager.getLogger(Record.class);

    public enum StartingPointPolicy {
        IMMEDIATE, AFTER_DELAY, EVENT_BASED, METADATA_BASED
    }

    protected DocumentModel document;

    public Record(DocumentModel doc) {
        document = doc;
    }

    public List<String> getBeginActions() {
        Serializable propertyValue = document.getPropertyValue(RetentionConstants.BEGIN_ACTIONS_PROP);
        if (propertyValue == null) {
            return Collections.emptyList();
        }
        return Arrays.asList((String[]) propertyValue);
    }

    public List<String> getDocTypes() {
        @SuppressWarnings("unchecked")
        List<String> propertyValue = (List<String>) document.getPropertyValue(RetentionConstants.DOC_TYPES_PROP);
        return propertyValue;
    }

    public void setDocTypes(List<String> types) {
        document.setPropertyValue(RetentionConstants.DOC_TYPES_PROP, (Serializable) types);
    }

    public boolean isDocTypeAccepted(String docType) {
        List<String> types = getDocTypes();
        return types == null || types.isEmpty() || types.contains(docType);
    }

    public DocumentModel getDocument() {
        return document;
    }

    public Long getDurationDays() {
        return (Long) document.getPropertyValue(RetentionConstants.DURATION_DAYS_PROP);
    }

    public Long getDurationMillis() {
        return (Long) document.getPropertyValue(RetentionConstants.DURATION_MILLIS_PROP);
    }

    public Long getDurationMonths() {
        return (Long) document.getPropertyValue(RetentionConstants.DURATION_MONTHS_PROP);
    }

    public Long getDurationYears() {
        return (Long) document.getPropertyValue(RetentionConstants.DURATION_YEARS_PROP);
    }

    public List<String> getEndActions() {
        Serializable propertyValue = document.getPropertyValue(RetentionConstants.END_ACTIONS_PROP);
        if (propertyValue == null) {
            return Collections.emptyList();
        }
        return Arrays.asList((String[]) propertyValue);
    }

    public String getExpression() {
        return (String) document.getPropertyValue(RetentionConstants.EXPRESSION_PROP);
    }

    public boolean isRetentionExpired() {
        if (!getDocument().isUnderRetentionOrLegalHold()) {
            return true;
        }
        Calendar retainUntil;
        return (retainUntil = getDocument().getRetainUntil()) == null || !Calendar.getInstance().before(retainUntil);
    }

    public boolean isRetainUntilInderterminate() {
        if (!getDocument().isUnderRetentionOrLegalHold()) {
            return false;
        }
        Calendar retainUntil = getDocument().getRetainUntil();
        return retainUntil != null
                ? CoreSession.RETAIN_UNTIL_INDETERMINATE.getTimeInMillis() == retainUntil.getTimeInMillis()
                : false;
    }

    public Calendar getRetainUntilDateFromNow() {
        LocalDateTime localDateTime = LocalDateTime.now()
                                                   .plusYears(getDurationYears())
                                                   .plusMonths(getDurationMonths())
                                                   .plusDays(getDurationDays())
                                                   .plusNanos(getDurationMillis() * 1000000);
        return GregorianCalendar.from(localDateTime.atZone(ZoneId.systemDefault()));
    }

    public String getStartingPointExpression() {
        return (String) document.getPropertyValue(RetentionConstants.STARTING_POINT_EXPRESSION_PROP);
    }

    public String getStartingPointEvent() {
        return (String) document.getPropertyValue(RetentionConstants.STARTING_POINT_EVENT_PROP);
    }

    public void setStartingPointEvent(String eventId) {
        document.setPropertyValue(RetentionConstants.STARTING_POINT_EVENT_PROP, eventId);
    }

    public void save(CoreSession session) {
        document.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        document.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, true);
        document.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, true);
        document.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, true);
        document.putContextData(RetentionConstants.RETENTION_CHECKER_LISTENER_IGNORE, true);
        session.saveDocument(document);
        document.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, null);
        document.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, null);
        document.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, null);
        document.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, null);
        document.getContextData().remove(RetentionConstants.RETENTION_CHECKER_LISTENER_IGNORE);
    }

    public void setBeginActions(List<String> actions) {
        document.setPropertyValue(RetentionConstants.BEGIN_ACTIONS_PROP, (Serializable) actions);
    }

    public void setDurationDays(Long days) {
        document.setPropertyValue(RetentionConstants.DURATION_DAYS_PROP, days);
    }

    public void setDurationMillis(long millis) {
        document.setPropertyValue(RetentionConstants.DURATION_MILLIS_PROP, millis);
    }

    public void setDurationMonths(Long months) {
        document.setPropertyValue(RetentionConstants.DURATION_MONTHS_PROP, months);
    }

    public void setDurationYears(Long years) {
        document.setPropertyValue(RetentionConstants.DURATION_YEARS_PROP, years);
    }

    public void setEndActions(List<String> actions) {
        document.setPropertyValue(RetentionConstants.END_ACTIONS_PROP, (Serializable) actions);
    }

    public void setExpression(String expression) {
        document.setPropertyValue(RetentionConstants.EXPRESSION_PROP, expression);
    }

    public void setStartingPointExpression(String expression) {
        document.setPropertyValue(RetentionConstants.STARTING_POINT_EXPRESSION_PROP, expression);
    }

    public String getStartingPointPolicy() {
        return (String) document.getPropertyValue(RetentionConstants.STARTING_POINT_POLICY_PROP);
    }

    public void setStartingPointPolicy(StartingPointPolicy policy) {
        document.setPropertyValue(RetentionConstants.STARTING_POINT_POLICY_PROP, policy.name().toLowerCase());
    }

    public boolean isAfterDely() {
        return StartingPointPolicy.AFTER_DELAY.name().toLowerCase().equals(getStartingPointPolicy());
    }

    public boolean isMetadataBased() {
        return StartingPointPolicy.METADATA_BASED.name().toLowerCase().equals(getStartingPointPolicy());
    }

    public boolean isImmediate() {
        return StartingPointPolicy.IMMEDIATE.name().toLowerCase().equals(getStartingPointPolicy());
    }

    public boolean isEventBased() {
        return StartingPointPolicy.EVENT_BASED.name().toLowerCase().equals(getStartingPointPolicy());
    }
}