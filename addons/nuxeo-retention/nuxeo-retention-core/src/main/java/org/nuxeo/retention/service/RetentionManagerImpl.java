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

import java.util.Calendar;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.document.DeleteDocument;
import org.nuxeo.ecm.automation.core.operations.document.LockDocument;
import org.nuxeo.ecm.automation.core.operations.document.TrashDocument;
import org.nuxeo.ecm.automation.core.operations.document.UnlockDocument;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.bulk.BulkServiceImpl;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 11.1
 */
public class RetentionManagerImpl extends DefaultComponent implements RetentionManager {

    private static final Logger log = LogManager.getLogger(BulkServiceImpl.class);

    @Override
    public DocumentModel getRetentionRulesRoot(CoreSession session) {
        DocumentModel retentionRulesRoot = session.createDocumentModel("/", RetentionConstants.RULES_CONTAINER_TYPE,
                RetentionConstants.RULES_CONTAINER_TYPE);
        return CoreInstance.doPrivileged(session, s -> {
            return session.getOrCreateDocument(retentionRulesRoot, doc -> initRetentionRulesRoot(session, doc));
        });
    }

    protected DocumentModel initRetentionRulesRoot(CoreSession session, DocumentModel doc) {
        ACP acp = new ACPImpl();
        ACE allowRead = new ACE(SecurityConstants.EVERYONE, SecurityConstants.READ, true);
        ACE allowEverything = new ACE(session.getPrincipal().getName(), SecurityConstants.EVERYTHING, true);
        ACE denyEverything = new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
        ACL acl = new ACLImpl();
        // TODO Check who can create Retention Rules, a retention manager role/group?
        acl.setACEs(new ACE[] { allowRead, allowEverything, denyEverything });
        acp.addACL(acl);
        doc.setACP(acp, true);
        return doc;
    }

    @Override
    public DocumentModel applyRule(DocumentModel document, DocumentModel ruleDocument, CoreSession session) {
        if (document.isRecord()) {
            throw new NuxeoException("Document is already a record");
        }
        if (!ruleDocument.hasFacet(RetentionConstants.RETENTION_RULE_FACET)) {
            throw new NuxeoException(String.format("Document is not a rule"));
        }
        RetentionRule rule = ruleDocument.getAdapter(RetentionRule.class);
        document.addFacet(RetentionConstants.RECORD_FACET);
        Record record = document.getAdapter(Record.class);
        rule.saveRetentionInfo(record);
        executeRuleBeginActions(record, session);
        session.saveDocument(document);
        session.makeRecord(document.getRef());
        if (rule.isManual()) {
            Calendar retainUntil = rule.getRetainUntilDateFromNow();
            log.debug("Setting retention on {} until {}", document::getPathAsString, retainUntil::getTime);
            session.setRetainUntil(document.getRef(), retainUntil);
        } else if (rule.isAuto()) {
            session.setRetainUntil(document.getRef(), CoreSession.RETAIN_UNTIL_INDETERMINATE);
        } else {
            throw new UnsupportedOperationException("Unsupported application policy");
        }
        return session.getDocument(document.getRef());
    }

    @Override
    public void executeRuleBeginActions(Record record, CoreSession session) {
        log.debug("Processing begin actions on {}", () -> record.getDocument().getPath());
        executeRuleActions(record.getDocument(), record::getBeginActions, session);
    }

    @Override
    public void executeRuleEndActions(Record record, CoreSession session) {
        executeRuleActions(record.getDocument(), record::getEndActions, session);
    }

    protected void executeRuleActions(DocumentModel doc, Supplier<String[]> actionIds, CoreSession session) {
        AutomationService automationService = Framework.getService(AutomationService.class);
        for (String operationId : actionIds.get()) {
            log.debug("Executing {} action on {}", () -> operationId, doc::getPathAsString);
            // Do not lock document if already locked, and unlock if already unlocked (triggers an error)
            // Also, if it's time to delete, unlock it first, etc.
            // (more generally, be ready to handle specific operations and context)
            switch (operationId) {
            case LockDocument.ID:
                if (doc.isLocked()) {
                    continue;
                }
                break;

            case UnlockDocument.ID:
                if (!doc.isLocked()) {
                    continue;
                }
                break;

            case DeleteDocument.ID:
            case TrashDocument.ID:
                if (doc.isLocked()) {
                    session.removeLock(doc.getRef());
                    doc = session.getDocument(doc.getRef());
                }
                break;
            }
            OperationContext context = getExecutionContext(doc, session);
            try {
                automationService.run(context, operationId);
            } catch (OperationException e) {
                throw new NuxeoException("Error running operation: " + operationId, e);
            }
        }
    }

    protected OperationContext getExecutionContext(DocumentModel doc, CoreSession session) {
        OperationContext context = new OperationContext(session);
        context.put("document", doc);
        context.setCommit(false); // no session save at end
        context.setInput(doc);
        return context;
    }

}
