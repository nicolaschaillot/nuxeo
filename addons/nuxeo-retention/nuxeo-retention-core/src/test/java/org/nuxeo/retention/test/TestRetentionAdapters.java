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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-core-types.xml")
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-adapters.xml")
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestRetentionAdapters {

    @Inject
    protected CoreSession session;

    @Test
    public void testRecordAdapterRetainDate() {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file.addFacet(RetentionConstants.RECORD_FACET);
        file = session.saveDocument(file);

        Record record = file.getAdapter(Record.class);
        assertFalse(record.isRetainUntilInderterminate());

        session.makeRecord(file.getRef());
        session.setRetainUntil(file.getRef(), CoreSession.RETAIN_UNTIL_INDETERMINATE);
        file = session.getDocument(file.getRef());

        record = file.getAdapter(Record.class);
        assertTrue(record.isRetainUntilInderterminate());
        assertFalse(record.isRetentionExpired());

        Calendar retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.HOUR, -1);
        session.setRetainUntil(file.getRef(), retainUntil);
        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        assertFalse(record.isRetainUntilInderterminate());
        assertTrue(record.isRetentionExpired());

        retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.HOUR, 2);
        session.setRetainUntil(file.getRef(), retainUntil);
        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        assertFalse(record.isRetainUntilInderterminate());
        assertFalse(record.isRetentionExpired());
    }

    @Test
    public void testRecordAdapterSartingPointPolicy() {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file.addFacet(RetentionConstants.RECORD_FACET);
        file = session.saveDocument(file);

        Record record = file.getAdapter(Record.class);

        record.setStartingPointPolicy(Record.StartingPointPolicy.IMMEDIATE);
        assertTrue(record.isImmediate());
        assertFalse(record.isAfterDely());
        assertFalse(record.isEventBased());
        assertFalse(record.isMetadataBased());

        record.setStartingPointPolicy(Record.StartingPointPolicy.AFTER_DELAY);
        assertFalse(record.isImmediate());
        assertTrue(record.isAfterDely());
        assertFalse(record.isEventBased());
        assertFalse(record.isMetadataBased());

        record.setStartingPointPolicy(Record.StartingPointPolicy.EVENT_BASED);
        assertFalse(record.isImmediate());
        assertFalse(record.isAfterDely());
        assertTrue(record.isEventBased());
        assertFalse(record.isMetadataBased());

        record.setStartingPointPolicy(Record.StartingPointPolicy.METADATA_BASED);
        assertFalse(record.isImmediate());
        assertFalse(record.isAfterDely());
        assertFalse(record.isEventBased());
        assertTrue(record.isMetadataBased());
    }

    @Test
    public void testMetadataXPathValidity() {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file.addFacet(RetentionConstants.RECORD_FACET);
        file = session.saveDocument(file);

        Record record = file.getAdapter(Record.class);

        try {
            record.setMetadataXpath("dc:title");
            fail("Metatada xpath should be of type Date");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

}
