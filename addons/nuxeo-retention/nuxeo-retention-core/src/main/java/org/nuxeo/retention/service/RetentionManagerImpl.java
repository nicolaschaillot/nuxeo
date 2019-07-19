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

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.el.ExpressionFactoryImpl;
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
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.workers.RuleEvaluationWorker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 11.1
 */
public class RetentionManagerImpl extends DefaultComponent implements RetentionManager {

    private static final Logger log = LogManager.getLogger(RetentionManagerImpl.class);

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
    public DocumentModel attachRule(DocumentModel document, RetentionRule rule, CoreSession session) {
        if (!rule.isEnabled()) {
            throw new NuxeoException(String.format("Rule is disabled"));
        }
        document.addFacet(RetentionConstants.RECORD_FACET);
        Record record = document.getAdapter(Record.class);
        rule.copyRetentionInfo(record);
        executeRuleBeginActions(record, session);
        session.saveDocument(document);
        session.makeRecord(document.getRef());
        Calendar retainUntil = rule.getRetainUntilDateFromNow();
        log.debug("Setting retention on {} until {}", document::getPathAsString,
                () -> retainUntil != null ? RetentionConstants.DEFAULT_DATE_FORMAT.format(retainUntil.getTime())
                        : "indeterminate");
        session.setRetainUntil(document.getRef(), retainUntil);
        record.save(session);
        return session.getDocument(document.getRef());
    }

    public void executeRuleBeginActions(Record record, CoreSession session) {
        log.debug("Processing begin actions on {}", () -> record.getDocument().getPath());
        executeRuleActions(record.getDocument(), record.getBeginActions(), session);
    }

    public void executeRuleEndActions(Record record, CoreSession session) {
        executeRuleActions(record.getDocument(), record.getEndActions(), session);
    }

    protected void executeRuleActions(DocumentModel doc, String[] actionIds, CoreSession session) {
        if (actionIds != null) {
            AutomationService automationService = Framework.getService(AutomationService.class);
            for (String operationId : actionIds) {
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
    }

    protected OperationContext getExecutionContext(DocumentModel doc, CoreSession session) {
        OperationContext context = new OperationContext(session);
        context.put("document", doc);
        context.setCommit(false); // no session save at end
        context.setInput(doc);
        return context;
    }

    @Override
    public void evalRules(Map<String, Set<String>> docsToCheckAndEvents) {
        if (docsToCheckAndEvents.isEmpty()) {
            return;
        }
        RuleEvaluationWorker work = new RuleEvaluationWorker(docsToCheckAndEvents);
        Framework.getService(WorkManager.class).schedule(work, WorkManager.Scheduling.ENQUEUE);
    }

    protected ELActionContext initActionContext(DocumentModel doc, CoreSession session) {
        // handles only filters that can be resolved in this context
        ELActionContext ctx = new ELActionContext(new ExpressionContext(), new ExpressionFactoryImpl());
        ctx.setDocumentManager(session);
        ctx.setCurrentPrincipal(session.getPrincipal());
        ctx.setCurrentDocument(doc);
        return ctx;
    }

    protected Boolean evaluateConditionExpression(ELActionContext ctx, String expression) {
        Calendar now = Calendar.getInstance();
        if (StringUtils.isEmpty(expression)) {
            return true;
        }
        ctx.putLocalVariable("currentDate", now);
        return ctx.checkCondition(expression);
    }

    @Override
    public void evalRules(Record record, Set<String> events, CoreSession session) {
        if (record == null) {
            return; // nothing to do
        }
        if (record.isRetentionExpired()) {
            // retention expired, nothing to do
            proceedRetentionExpired(record, session);
            return;

        }
        for (String event : events) {
            if (event.equals(record.getStartingPointEvent())) {
                ELActionContext actionContext = initActionContext(record.getDocument(), session);
                String expression = record.getStartingPointExpression();
                Boolean startNow = evaluateConditionExpression(actionContext, record.getStartingPointExpression());
                if (startNow) {
                    session.setRetainUntil(record.getDocument().getRef(), record.getRetainUntilDateFromNow(true));
                } else {
                    log.debug("The '{}' expression evaluated to false", expression);
                }
                break;
            }
        }
    }

    @Override
    public void proceedRetentionExpired(Record record, CoreSession session) {
        executeRuleEndActions(record, session);
        record.getDocument().removeFacet(RetentionConstants.RECORD_FACET);
    }

    protected List<String> acceptedEvents;

    @Override
    public List<String> getAcceptedEvents() {
        if (acceptedEvents == null) {
            synchronized (this) {
                if (acceptedEvents == null) {
                    DirectoryService directoryService = Framework.getService(DirectoryService.class);
                    Directory dir = directoryService.getDirectory(RetentionConstants.EVENTS_DIRECTORY_NAME);
                    try (Session session = dir.getSession()) {
                        Map<String, Serializable> filter = new HashMap<>();
                        filter.put(RetentionConstants.OBSOLETE_FIELD_ID, Long.valueOf(0));
                        acceptedEvents = session.getProjection(filter, session.getIdField());
                        log.debug("Accepted events {}", acceptedEvents::toString);
                    }
                }

            }
        }
        return acceptedEvents;
    }

    @Override
    public void invalidate() {
        this.acceptedEvents = null;
    };

}
