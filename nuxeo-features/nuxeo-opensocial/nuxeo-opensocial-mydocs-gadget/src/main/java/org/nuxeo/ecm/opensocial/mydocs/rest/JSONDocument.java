/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.opensocial.mydocs.rest;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.DocumentsPageProvider;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "JSONDocument", superType = "Document")
public class JSONDocument extends DocumentObject {

    private static final int PAGE_SIZE = 10;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    private final static CompoundFilter FILTER = new CompoundFilter(
            new FacetFilter(FacetNames.HIDDEN_IN_NAVIGATION, false),
            new LifeCycleFilter(LifeCycleConstants.DELETED_STATE, false));

    private static final Log log = LogFactory.getLog(JSONDocument.class);

    @GET
    @Override
    public Object doGet() {
        String currentPage = ctx.getRequest()
                .getParameter("page");

        Integer index = 0;
        try {
            int _ind = Integer.valueOf(currentPage);
            index = (_ind < 0) ? index : _ind;
        } catch (Exception e) {
        }

        Map<String, Object> all = new HashMap<String, Object>();
        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("id", getDocument().getId());
        try {
            summary.put("title", getDocument().getTitle());
        } catch (Exception e) {
            summary.put("title", "No title");
        }

        CoreSession session = ctx.getCoreSession();

        PagedDocumentsProvider provider;
        try {
            provider = getResProviderForDocChildren(getDocument().getRef(),
                    session);
            summary.put("pages", fixedSQLExceptionWhenUpload(session,
                    provider.getNumberOfPages()));
            summary.put("pageNumber", index);
            summary.put("id", getDocument().getRef()
                    .toString());
            all.put("summary", summary);

            List<Object> docs = new ArrayList<Object>();

            for (DocumentModel child : provider.getPage(index)) {
                // FIXME
                if (!"Space".equals(child.getType())) {
                    docs.add(getDocItem(child));
                }
            }
            all.put("document", docs);

            return makeJSON(all);
        } catch (ClientException e) {
            log.error(e, e);
        }

        return Response.serverError()
                .build();

    }

    @POST
    @Override
    public Response doPost() {
        CoreSession session = ctx.getCoreSession();

        FileManager fm;
        try {
            fm = Framework.getService(FileManager.class);
        } catch (Exception e1) {
            log.error(e1, e1);
            return Response.serverError()
                    .build();
        }

        try {
            FormData form = ctx.getForm();
            Blob blob = form.getFirstBlob();
            if (blob == null) {
                throw new IllegalArgumentException(
                        "Could not find any uploaded file");
            }

            blob.persist();
            fm.createDocumentFromBlob(session, blob, doc.getPathAsString(),
                    true, blob.getFilename());
        } catch (IOException e) {
            log.error(e, e);
        } catch (Exception e) {
            log.error(e, e);
            // TODO : Resolve this exception...
            // org.nuxeo.ecm.core.storage.sql.Fragment.setPristine(Fragment.java:394)
            // at org.nuxeo.ecm.core.storage.sql.Context.save(Context.java:523)
            // org.nuxeo.ecm.core.storage.sql.PersistenceContext.save(PersistenceContext.java:322)
            // org.nuxeo.ecm.core.storage.sql.SessionImpl.flush(SessionImpl.java:223)
            // org.nuxeo.ecm.core.storage.sql.SessionImpl.save(SessionImpl.java:208)
            // org.nuxeo.ecm.core.storage.sql.coremodel.SQLSession.save(SQLSession.java:135)
            // org.nuxeo.ecm.core.api.AbstractSession.save(AbstractSession.java:1692)
            // org.nuxeo.ecm.platform.filemanager.service.extension.DefaultFileImporter.create(DefaultFileImporter.java:99)
            // org.nuxeo.ecm.platform.filemanager.service.FileManagerService.createDocumentFromBlob(FileManagerService.java:248)
            // org.nuxeo.ecm.opensocial.mydocs.rest.JSONDocument.doPost(JSONDocument.java:156)
            // Fixed in method fixedSQLExceptionWhenUpload
        }

        return Response.ok("File upload ok!", MediaType.TEXT_PLAIN)
                .build();
    }

    private int fixedSQLExceptionWhenUpload(CoreSession session, int nbPages)
            throws ClientException {
        if (nbPages < 1) {
            // FIXME
            log.warn("nbPages in PagedDocumentsProvider < 1");
            DocumentModelList children = session.getChildren(
                    getDocument().getRef(), null, SecurityConstants.READ,
                    FILTER, null);
            nbPages = (int) Math.ceil(children.size() / new Double(PAGE_SIZE));
            log.warn("nbPages is " + nbPages);
        }
        return nbPages;
    }

    private Map<String, Object> getDocItem(DocumentModel doc) {
        Map<String, Object> docItem = new HashMap<String, Object>();
        docItem.put("id", doc.getId());
        docItem.put("name", doc.getName());
        docItem.put("url", getDocumentURL(doc));
        docItem.put("type", doc.getType());
        try {
            docItem.put("title", doc.getTitle());
        } catch (ClientException e1) {
            log.warn("No title for document " + doc.getName());
            docItem.put("title", "No title");
        }
        try {
            docItem.put("icon", doc.getPropertyValue("common:icon"));
        } catch (Exception e) {
            log.warn("No icon for document " + doc.getName());
            docItem.put("icon", "No icon");
        }
        try {
            docItem.put("creator", doc.getPropertyValue("dublincore:creator"));
        } catch (Exception e) {
            log.warn("No creator for document " + doc.getName());
            docItem.put("creator", "No creator");
        }
        try {
            docItem.put(
                    "modified",
                    DATE_FORMAT.format(((GregorianCalendar) doc.getPropertyValue("dublincore:modified")).getTime()));
        } catch (Exception e) {
            log.warn("No modified for document " + doc.getName());
            docItem.put("modified", "No modified");
        }

        docItem.put("folderish", doc.hasFacet("Folderish") ? "1" : "0");
        return docItem;
    }

    @Path(value = "{path}")
    public Resource traverse(@PathParam("path") String path) {
        return newDocument(path);
    }

    public DocumentObject newDocument(String path) {
        try {
            PathRef pathRef = new PathRef(doc.getPath()
                    .append(path)
                    .toString());
            DocumentModel doc = ctx.getCoreSession()
                    .getDocument(pathRef);
            return (DocumentObject) newObject("JSONDocument", doc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    private PagedDocumentsProvider getResProviderForDocChildren(
            DocumentRef docRef, CoreSession session) throws ClientException {

        DocumentModelIterator resultDocsIt = session.getChildrenIterator(
                docRef, null, SecurityConstants.READ, FILTER);

        return new DocumentsPageProvider(resultDocsIt, PAGE_SIZE);
    }

    protected static String makeJSON(Map<String, Object> all) {
        JSON jsonRes = JSONSerializer.toJSON(all);
        if (jsonRes instanceof JSONObject) {
            JSONObject jsonOb = (JSONObject) jsonRes;
            return jsonOb.toString(2);
        } else if (jsonRes instanceof JSONArray) {
            JSONArray jsonOb = (JSONArray) jsonRes;
            return jsonOb.toString(2);
        } else {
            return null;
        }
    }

    protected static String getDocumentURL(DocumentModel doc) {
        DocumentViewCodecManager dvcm;
        try {
            dvcm = Framework.getService(DocumentViewCodecManager.class);
            return dvcm.getUrlFromDocumentView(new DocumentViewImpl(doc),
                    false, null);
        } catch (Exception e) {
            return null;
        }
    }

}