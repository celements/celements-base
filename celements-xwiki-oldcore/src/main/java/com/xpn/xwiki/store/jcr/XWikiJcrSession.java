/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package com.xpn.xwiki.store.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.QueryManager;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.portals.graffito.jcr.exception.PersistenceException;
import org.apache.portals.graffito.jcr.persistence.PersistenceManager;
import org.apache.portals.graffito.jcr.persistence.objectconverter.impl.ObjectConverterImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/** helper jcr.Session subclass */
public class XWikiJcrSession implements Session {

  private Session s;
  private PersistenceManager pm;
  private ObjectConverterImpl oc;

  public XWikiJcrSession(Session s, PersistenceManager pm, ObjectConverterImpl objectConverter) {
    this.s = s;
    this.pm = pm;
    this.oc = objectConverter;
  }

  /** Get xwiki:store node */
  public Node getStoreNode() throws RepositoryException {
    try {
      return getRootNode().getNode("store");
    } catch (PathNotFoundException e) {
      return getRootNode().addNode("store", "xwiki:store");
    }
  }

  public Node getNode(String path) throws PathNotFoundException, RepositoryException {
    return (Node) s.getItem(path);
  }

  /** Insert persistent object using graffito jcr-mapping */
  public Node insertObject(Node parentNode, String nodeName, Object object) {
    return oc.insert(s, parentNode, ISO9075.encode(nodeName), object);
  }

  /** Insert persistent object using graffito jcr-mapping. low-level function */
  public Node insertObject(Node parentNode, String nodeName, Object object, Class objclass) {
    return oc.insert(s, parentNode, ISO9075.encode(nodeName), object, objclass);
  }

  /** Update persistent object using graffito jcr-mapping */
  public void updateObject(Node node, Object object) throws PersistenceException,
      ItemNotFoundException, AccessDeniedException, RepositoryException {
    oc.update(s, node, object);
  }

  /** Update persistent object using graffito jcr-mapping. low-level function */
  public void updateObject(Node node, Object object, Class objclass) throws PersistenceException,
      ItemNotFoundException, AccessDeniedException, RepositoryException {
    oc.update(s, node, object, objclass);
  }

  public Object loadObject(String path) {
    return pm.getObject(path);
  }

  public void removeObject(String path) {
    if (isNodeExist(path)) {
      pm.remove(path);
    }
  }

  public boolean isNodeExist(String path) {
    return pm.objectExists(path);
  }

  public Session getJcrSession() {
    return s;
  }

  public QueryManager getQueryManager() throws RepositoryException {
    return s.getWorkspace().getQueryManager();
  }

  public org.apache.portals.graffito.jcr.query.QueryManager getObjectQueryManager() {
    return pm.getQueryManager();
  }

  // subclassed methods
  /** @inheritDoc */
  @Override
  public Repository getRepository() {
    return s.getRepository();
  }

  /** @inheritDoc */
  @Override
  public String getUserID() {
    return s.getUserID();
  }

  /** @inheritDoc */
  @Override
  public Object getAttribute(String arg0) {
    return s.getAttribute(arg0);
  }

  /** @inheritDoc */
  @Override
  public String[] getAttributeNames() {
    return s.getAttributeNames();
  }

  /** @inheritDoc */
  @Override
  public Workspace getWorkspace() {
    return s.getWorkspace();
  }

  /** @inheritDoc */
  @Override
  public Session impersonate(Credentials arg0) throws LoginException,
      RepositoryException {
    return s.impersonate(arg0);
  }

  /** @inheritDoc */
  @Override
  public Node getRootNode() throws RepositoryException {
    return s.getRootNode();
  }

  /** @inheritDoc */
  @Override
  public Node getNodeByUUID(String arg0) throws ItemNotFoundException,
      RepositoryException {
    return s.getNodeByUUID(arg0);
  }

  /** @inheritDoc */
  @Override
  public Item getItem(String arg0) throws PathNotFoundException,
      RepositoryException {
    return s.getItem(arg0);
  }

  /** @inheritDoc */
  @Override
  public boolean itemExists(String arg0) throws RepositoryException {
    return s.itemExists(arg0);
  }

  /** @inheritDoc */
  @Override
  public void move(String arg0, String arg1) throws ItemExistsException,
      PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException {
    s.move(arg0, arg1);
  }

  /** @inheritDoc */
  @Override
  public void save() throws AccessDeniedException, ItemExistsException,
      ConstraintViolationException, InvalidItemStateException,
      VersionException, LockException, NoSuchNodeTypeException,
      RepositoryException {
    pm.save();
    s.save();
  }

  /** @inheritDoc */
  @Override
  public void refresh(boolean arg0) throws RepositoryException {
    s.refresh(arg0);
  }

  /** @inheritDoc */
  @Override
  public boolean hasPendingChanges() throws RepositoryException {
    return s.hasPendingChanges();
  }

  /** @inheritDoc */
  @Override
  public ValueFactory getValueFactory()
      throws UnsupportedRepositoryOperationException, RepositoryException {
    return s.getValueFactory();
  }

  /** @inheritDoc */
  @Override
  public void checkPermission(String arg0, String arg1)
      throws AccessControlException, RepositoryException {
    s.checkPermission(arg0, arg1);
  }

  /** @inheritDoc */
  @Override
  public ContentHandler getImportContentHandler(String arg0, int arg1)
      throws PathNotFoundException, ConstraintViolationException,
      VersionException, LockException, RepositoryException {
    return s.getImportContentHandler(arg0, arg1);
  }

  /** @inheritDoc */
  @Override
  public void importXML(String arg0, InputStream arg1, int arg2)
      throws IOException, PathNotFoundException, ItemExistsException,
      ConstraintViolationException, VersionException,
      InvalidSerializedDataException, LockException, RepositoryException {
    s.importXML(arg0, arg1, arg2);
  }

  /** @inheritDoc */
  @Override
  public void exportSystemView(String arg0, ContentHandler arg1,
      boolean arg2, boolean arg3) throws PathNotFoundException,
      SAXException, RepositoryException {
    s.exportSystemView(arg0, arg1, arg2, arg3);
  }

  /** @inheritDoc */
  @Override
  public void exportSystemView(String arg0, OutputStream arg1, boolean arg2,
      boolean arg3) throws IOException, PathNotFoundException,
      RepositoryException {
    s.exportSystemView(arg0, arg1, arg2, arg3);
  }

  /** @inheritDoc */
  @Override
  public void exportDocumentView(String arg0, ContentHandler arg1,
      boolean arg2, boolean arg3) throws PathNotFoundException,
      SAXException, RepositoryException {
    s.exportDocumentView(arg0, arg1, arg2, arg3);
  }

  /** @inheritDoc */
  @Override
  public void exportDocumentView(String arg0, OutputStream arg1,
      boolean arg2, boolean arg3) throws IOException,
      PathNotFoundException, RepositoryException {
    s.exportDocumentView(arg0, arg1, arg2, arg3);
  }

  /** @inheritDoc */
  @Override
  public void setNamespacePrefix(String arg0, String arg1)
      throws NamespaceException, RepositoryException {
    s.setNamespacePrefix(arg0, arg1);
  }

  /** @inheritDoc */
  @Override
  public String[] getNamespacePrefixes() throws RepositoryException {
    return s.getNamespacePrefixes();
  }

  /** @inheritDoc */
  @Override
  public String getNamespaceURI(String arg0) throws NamespaceException,
      RepositoryException {
    return s.getNamespaceURI(arg0);
  }

  /** @inheritDoc */
  @Override
  public String getNamespacePrefix(String arg0) throws NamespaceException,
      RepositoryException {
    return s.getNamespacePrefix(arg0);
  }

  /** @inheritDoc */
  @Override
  public void logout() {
    pm.logout();
  }

  /** @inheritDoc */
  @Override
  public boolean isLive() {
    return s.isLive();
  }

  /** @inheritDoc */
  @Override
  public void addLockToken(String arg0) {
    s.addLockToken(arg0);
  }

  /** @inheritDoc */
  @Override
  public String[] getLockTokens() {
    return s.getLockTokens();
  }

  /** @inheritDoc */
  @Override
  public void removeLockToken(String arg0) {
    s.removeLockToken(arg0);
  }
}
