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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.retention.RetentionConstants;

/**
 * @since 11.1
 */
public class Record {

    protected DocumentModel document;

    public DocumentModel getDocument() {
        return document;
    }

    public Record(DocumentModel doc) {
        document = doc;
    }

    public Integer getDurationDays() {
        return (Integer) document.getPropertyValue(RetentionConstants.DURATION_DAYS_PROP);
    }

    public Integer getDurationMonths() {
        return (Integer) document.getPropertyValue(RetentionConstants.DURATION_MONTHS_PROP);
    }

    public Integer getDurationYears() {
        return (Integer) document.getPropertyValue(RetentionConstants.DURATION_YEARS_PROP);
    }

    public void setDurationDays(Integer days) {
        document.setPropertyValue(RetentionConstants.DURATION_DAYS_PROP, days);
    }

    public void setDurationMonths(Integer months) {
        document.setPropertyValue(RetentionConstants.DURATION_MONTHS_PROP, months);
    }

    public void setDurationYears(Integer years) {
        document.setPropertyValue(RetentionConstants.DURATION_YEARS_PROP, years);
    }

    public String getExpression() {
        return (String) document.getPropertyValue(RetentionConstants.EXPRESSION_PROP);
    }

    public void setExpression(String expression) {
        document.setPropertyValue(RetentionConstants.EXPRESSION_PROP, expression);
    }

    public String[] getBeginActions() {
        return (String[]) document.getPropertyValue(RetentionConstants.BEGIN_ACTIONS_PROP);
    }

    public void setEndActions(String[] actions) {
        document.setPropertyValue(RetentionConstants.END_ACTIONS_PROP, actions);
    }

    public void setBeginActions(String[] actions) {
        document.setPropertyValue(RetentionConstants.BEGIN_ACTIONS_PROP, actions);
    }

    public String[] getEndActions() {
        return (String[]) document.getPropertyValue(RetentionConstants.END_ACTIONS_PROP);
    }

    public Calendar getRetainUntilDateFromNow() {
        LocalDateTime localDateTime = LocalDateTime.now();
        if (getDurationYears() != null) {
            localDateTime.plusYears(getDurationYears());
        }
        if (getDurationMonths() != null) {
            localDateTime.plusMonths(getDurationMonths());
        }
        if (getDurationDays() != null) {
            localDateTime.plusDays(getDurationDays());
        }
        if (getDurationMillis() != null) {
            localDateTime.plusNanos(getDurationMillis() * 1000000);
        }
        return GregorianCalendar.from(localDateTime.atZone(ZoneId.systemDefault()));
    }

    public void setDurationMillis(long millis) {
        document.setPropertyValue(RetentionConstants.DURATION_MILLIS_PROP, millis);
    }

    public Long getDurationMillis() {
        return (Long) document.getPropertyValue(RetentionConstants.DURATION_MILLIS_PROP);
    }

}