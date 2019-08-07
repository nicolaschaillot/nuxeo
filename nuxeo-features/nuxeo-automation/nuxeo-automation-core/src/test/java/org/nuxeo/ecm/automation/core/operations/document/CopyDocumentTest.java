/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.google.inject.Inject;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class CopyDocumentTest {

    private static final String COPY_PATH = "/";

    private static final String COPY_DOC_NAME = "copyDoc";

    private static final String TARGET_PROPERTY_KEY = "target";

    private static final String NAME_PROPERTY_KEY = "name";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    @Inject
    protected TransactionalFeature txFeature;

    protected DocumentModel source;

    protected OperationContext context;

    @Before
    public void setUp() {
        source = session.createDocumentModel(COPY_PATH, "Source", "File");
        source = session.createDocument(source);
        session.save();

        context = new OperationContext(session);
    }

    @After
    public void tearDown() {
        context.close();
        session.removeChildren(session.getRootDocument().getRef());
        session.save();
    }

    @Test
    public void testLifeCycleResetOnCopy() throws OperationException {
        String initialLifeCycleState = source.getCurrentLifeCycleState();

        // Change the source current lifecycle state
        source.followTransition("approve");
        source = session.saveDocument(source);
        txFeature.nextTransaction();

        // Call Document.Copy without resetting the lifecycle state of the copy
        Map<String, Serializable> params = Map.of(TARGET_PROPERTY_KEY, COPY_PATH, NAME_PROPERTY_KEY, COPY_DOC_NAME,
                CoreEventConstants.RESET_LIFECYCLE, false);

        context.setInput(source);
        DocumentModel result = (DocumentModel) service.run(context, CopyDocument.ID, params);
        txFeature.nextTransaction();

        result = session.getDocument(result.getRef());
        assertEquals("approved", result.getCurrentLifeCycleState());

        // Call Document.Copy and reset the lifecycle state of the copy
        params = Map.of(TARGET_PROPERTY_KEY, COPY_PATH, NAME_PROPERTY_KEY, COPY_DOC_NAME,
                CoreEventConstants.RESET_LIFECYCLE, true);

        context.setInput(source);
        result = (DocumentModel) service.run(context, CopyDocument.ID, params);
        txFeature.nextTransaction();

        result = session.getDocument(result.getRef());

        assertEquals(initialLifeCycleState, result.getCurrentLifeCycleState());
    }
}
