/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = CopyDocument.ID, label = "Copy", category = Constants.CAT_DOCUMENT, description = "Copy the input document into the given Folderish. The name parameter will be used as the copy name otherwise if not specified the original name will be preserved. The target Folderish can be specified as an absolute or relative path (relative to the input document) as an UID or by using an EL expression. Return the newly created document (the copy). If the input document is a Folderish, all its content is copied recursively.")
public class CopyDocument {

    public static final String ID = "Document.Copy";

    @Context
    protected CoreSession session;

    @Param(name = "target")
    protected DocumentRef target; // the path or the ID

    @Param(name = "name", required = false)
    protected String name;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        String n = name;
        if (name == null || name.length() == 0) {
            n = doc.getName();
        }
        return session.copy(doc.getRef(), target, n);
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef ref) {
        String n = name;
        if (name == null || name.length() == 0) {
            n = session.getDocument(ref).getName();
        }
        return session.copy(ref, target, n);
    }
}
