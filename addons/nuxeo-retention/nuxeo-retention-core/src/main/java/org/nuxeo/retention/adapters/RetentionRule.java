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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.retention.RetentionConstants;

/**
 * @since 11.1
 */
public class RetentionRule extends Record {

    public enum Policy {
        AUTO, MANUAL
    }

    public RetentionRule(DocumentModel doc) {
        super(doc);
    }

    public boolean isAuto() {
        return Policy.AUTO.name().toLowerCase().equals(getApplicationPolicy());
    }

    public boolean isManual() {
        return Policy.MANUAL.name().toLowerCase().equals(getApplicationPolicy());
    }

    public String getApplicationPolicy() {
        return (String) document.getPropertyValue(RetentionConstants.APPLICATION_POLICY_PROP);
    }

    public void setApplicationPolicy(Policy policy) {
        document.setPropertyValue(RetentionConstants.APPLICATION_POLICY_PROP, policy.name().toLowerCase());
    }

    public void saveRetentionInfo(Record record) {
        record.setDurationDays(getDurationYears());
        record.setDurationMonths(getDurationMonths());
        record.setDurationDays(getDurationDays());
        record.setDurationMillis(getDurationMillis());
        record.setBeginActions(getBeginActions());
        record.setEndActions(getEndActions());
        if (isAuto()) {
            record.setExpression(getExpression());
        }
    }

}
