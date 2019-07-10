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
package org.nuxeo.retention.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.security.RetentionExpiredFinderListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.service.RetentionManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, AutomationFeature.class })
@Deploy({ "org.nuxeo.retention.core" })
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

    DocumentModel testRuleDoc;

    @Before
    public void setup() {
        testRuleDoc = session.createDocumentModel("/RetentionRules", "testRule", "RetentionRule");
        testRuleDoc = session.createDocument(testRuleDoc);
        RetentionRule rule = testRuleDoc.getAdapter(RetentionRule.class);
        rule.setApplicationPolicy(RetentionRule.Policy.MANUAL);
        rule.setDurationMillis(1_000);
        rule.setBeginActions(new String[] { "Document.Lock" });
        rule.setEndActions(new String[] { "Document.Unlock" });
        testRuleDoc = session.saveDocument(rule.getDocument());
    }

    @Test
    public void attachManualRule() throws InterruptedException {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        file = session.saveDocument(file);
        file = service.applyRule(file, testRuleDoc, session);
        assertTrue(file.isRecord());
        assertTrue(file.isLocked());

        // wait a bit more than retention period to pass retention expiration date
        nextTransaction();
        Thread.sleep(testRuleDoc.getAdapter(RetentionRule.class).getDurationMillis() + 1);
        // trigger manually instead of waiting for scheduler
        new RetentionExpiredFinderListener().handleEvent(null);
        // wait for all bulk commands to be executed
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));

        // re-acquire the doc in a new transaction
        nextTransaction();
        file = session.getDocument(file.getRef());

        // it has no retention anymore and can be deleted
        assertNull(session.getRetainUntil(file.getRef()));
        assertFalse(file.isLocked());
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }
}
