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
package org.nuxeo.retention;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @since 11.1
 */
public class RetentionConstants {

    public static final String RULES_CONTAINER_TYPE = "RetentionRules";

    public static final String DURATION_DAYS_PROP = "retention_def:durationDays";

    public static final String DURATION_MONTHS_PROP = "retention_def:durationMonths";

    public static final String DURATION_YEARS_PROP = "retention_def:durationYears";

    public static final String RECORD_FACET = "Record";

    public static final String RETENTION_RULE_FACET = "RetentionRule";

    public static final String APPLICATION_POLICY_PROP = "retention_rule:applicationPolicy";

    public static final String ENABLED_PROP = "retention_rule:enabled";

    public static final String EXPRESSION_PROP = "retention_def:expression";

    public static final String BEGIN_ACTIONS_PROP = "retention_def:beginActions";

    public static final String END_ACTIONS_PROP = "retention_def:endActions";

    public static final String DURATION_MILLIS_PROP = "retention_def:durationMillis";

    public static final String EVENTS_DIRECTORY_NAME = "RetentionEvent";

    public static final String OBSOLETE_FIELD_ID = "obsolete";

    public static final String RETENTION_CHECKER_LISTENER_IGNORE = "retentionRecordIgnore";

    public static final String STARTING_POINT_POLICY_PROP = "retention_def:startingPointPolicy";

    public static final String STARTING_POINT_EXPRESSION_PROP = "retention_def:startingPointExpression";

    public static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss.SSS");

    public static final String STARTING_POINT_EVENT_PROP = "retention_def:startingPointEvent";

    public static final String RECORD_MANAGER_GROUP_NAME = "RecordManager";

    public static final String MANAGE_RECORD_PERMISSION = "ManageRecord";

    public static final String DOC_TYPES_PROP = "retention_rule:docTypes";

}
