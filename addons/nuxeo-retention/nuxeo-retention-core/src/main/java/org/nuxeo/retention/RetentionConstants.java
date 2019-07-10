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

    public static final String EXPRESSION_PROP = "retention_def:expression";

    public static final String BEGIN_ACTIONS_PROP = "retention_def:beginActions";

    public static final String END_ACTIONS_PROP = "retention_def:endActions";

    public static final String DURATION_MILLIS_PROP = "retention_def:durationMillis";

}
