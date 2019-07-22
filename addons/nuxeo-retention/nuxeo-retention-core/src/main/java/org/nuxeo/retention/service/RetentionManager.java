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
package org.nuxeo.retention.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;

/**
 * @since 11.1
 */
public interface RetentionManager {

    DocumentModel getRetentionRulesRoot(CoreSession session);

    DocumentModel attachRule(DocumentModel document, RetentionRule rule, CoreSession session);

    /*
     * void executeRuleBeginActions(Record record, CoreSession session); void executeRuleEndActions(Record record,
     * CoreSession session);
     */

    void evalRules(Map<String, Set<String>> docsToCheckAndEvents);

    void evalRules(Record record, Set<String> events, CoreSession session);

    void proceedRetentionExpired(Record record, CoreSession coreSession);

    List<String> getAcceptedEvents();

}
