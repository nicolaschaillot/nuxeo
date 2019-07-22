/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this doc except in compliance with the License.
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
package org.nuxeo.retention.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.security.RetentionExpiredFinderListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.Record.StartingPointPolicy;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.service.RetentionManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class, TransactionalFeature.class, AutomationFeature.class })
@Deploy("org.nuxeo.retention.core")
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestRetentionManager {

    public static Log log = LogFactory.getLog(TestRetentionManager.class);

    @Inject
    protected CoreSession session;

    @Inject
    protected RetentionManager service;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected BulkService bulkService;

    protected DocumentModel file;

    protected void assertStillUnderRetentionAfter(DocumentModel doc, RetentionRule rule, int timeoutMillis)
            throws InterruptedException {
        doc = service.attachRule(doc, rule, session);
        assertTrue(doc.isRecord());

        awaitRetentionExpiration(1_000);

        doc = session.getDocument(doc.getRef());

        // it still has a retention date
        assertNotNull(session.getRetainUntil(doc.getRef()));
    }

    private void awaitRetentionExpiration(long millis) throws InterruptedException {
        // wait a bit more than retention period to pass retention expiration date
        coreFeature.waitForAsyncCompletion();
        Thread.sleep(millis);
        // trigger manually instead of waiting for scheduler
        new RetentionExpiredFinderListener().handleEvent(null);
        coreFeature.waitForAsyncCompletion();
    }

    protected RetentionRule createRuleWithActions(RetentionRule.ApplicationPolicy policy,
            StartingPointPolicy startingPointPolicy, String startingPointEventId, String startingPointExpression,
            long years, long months, long days, long durationMillis, String[] beginActions, String[] endActions) {
        DocumentModel doc = session.createDocumentModel("/RetentionRules", "testRule", "RetentionRule");
        RetentionRule rule = doc.getAdapter(RetentionRule.class);
        rule.setDurationYears(years);
        rule.setDurationMonths(months);
        rule.setDurationDays(days);
        rule.setApplicationPolicy(policy);
        rule.setStartingPointPolicy(startingPointPolicy);
        rule.setStartingPointEvent(startingPointEventId);
        rule.setStartingPointExpression(startingPointExpression);
        rule.setDurationMillis(durationMillis);
        rule.setBeginActions(beginActions);
        rule.setEndActions(endActions);
        doc = session.createDocument(doc);
        return session.saveDocument(rule.getDocument()).getAdapter(RetentionRule.class);
    }

    protected RetentionRule createImmediateRuleMillis(RetentionRule.ApplicationPolicy policy, long durationMillis,
            String[] beginActions, String[] endActions) {
        return createRuleWithActions(policy, RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, 0L, 0L, 0L,
                durationMillis, beginActions, endActions);
    }

    protected RetentionRule createManualImmediateRuleMillis(long durationMillis) {
        return createImmediateRuleMillis(RetentionRule.ApplicationPolicy.MANUAL, durationMillis, null, null);
    }

    protected RetentionRule createManualEventBasedRuleMillis(String eventId, String startingPointExpression,
            long durationMillis) {
        return createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.EVENT_BASED, eventId, startingPointExpression, 0L, 0L, 0L,
                durationMillis, null, null);
    }

    @Before
    public void setup() {
        file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file = session.saveDocument(file);
    }

    @Test
    public void test1DayManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, 0L, 0L, 1L, 0L, null, null), 1_000);
    }

    @Test
    public void test1MonthManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, 0L, 1L, 0L, 0L, null, null), 1_000);
    }

    @Test
    public void test1YearManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, 1L, 0L, 0L, 0L, null, null), 1_000);
    }

    @Test
    public void testManualImmediateRuleWithActions() throws InterruptedException {
        RetentionRule testRule = createImmediateRuleMillis(RetentionRule.ApplicationPolicy.MANUAL, 100L,
                new String[] { "Document.Lock" }, new String[] { "Document.Unlock" });

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertNotNull(session.getRetainUntil(file.getRef()));
        assertTrue(file.isLocked());

        awaitRetentionExpiration(1000L);

        file = session.getDocument(file.getRef());

        // it has no retention anymore
        assertNull(session.getRetainUntil(file.getRef()));
        assertFalse(file.isLocked());
    }

    @Test
    public void testManualImmediateRule() throws InterruptedException {
        RetentionRule testRule = createManualImmediateRuleMillis(100L);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertNotNull(session.getRetainUntil(file.getRef()));
        assertFalse(file.isLocked());

        awaitRetentionExpiration(1000L);

        file = session.getDocument(file.getRef());

        // it has no retention anymore
        assertNull(session.getRetainUntil(file.getRef()));
    }

    @Test
    public void testManualDocumentMoveToFolderRule() throws InterruptedException {

        RetentionRule testRule = createManualEventBasedRuleMillis(DocumentEventTypes.DOCUMENT_MOVED,
                "document.getPathAsString().startsWith('/testFolder')", 1000L);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        Record record = file.getAdapter(Record.class);
        assertTrue(record.isRetainUntilInderterminate());

        awaitRetentionExpiration(500L);

        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);
        assertTrue(record.isRetainUntilInderterminate());

        DocumentModel folder = session.createDocumentModel("/", "testFolder", "Folder");
        folder = session.createDocument(folder);
        folder = session.saveDocument(folder);

        file = session.move(file.getRef(), folder.getRef(), null);

        awaitRetentionExpiration(500L);

        record = file.getAdapter(Record.class);
        assertFalse(record.isRetainUntilInderterminate());
        assertFalse(record.isRetentionExpired());

        awaitRetentionExpiration(500L);

        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        // it has no retention anymore
        assertTrue(record.isRetentionExpired());
    }
}
