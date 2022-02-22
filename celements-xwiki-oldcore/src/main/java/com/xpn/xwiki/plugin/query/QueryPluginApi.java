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

package com.xpn.xwiki.plugin.query;

import javax.jcr.ValueFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.store.XWikiStoreInterface;

/** Api for QueryPlugin */
public class QueryPluginApi extends Api implements IQueryFactory {

  private static final Log log = LogFactory.getLog(QueryPluginApi.class);
  QueryPlugin qp;

  /**
   * @deprecated This version si buggy since it use the initial context of the plugin and
   *             not the current context
   */
  @Deprecated
  public QueryPluginApi(QueryPlugin qp) {
    this(qp, qp.getContext());
  }

  public QueryPluginApi(QueryPlugin qp, XWikiContext context) {
    super(context);
    this.qp = qp;
  }

  @Override
  public IQuery getDocs(String docname, String prop, String order) throws XWikiException {
    return qp.getDocs(docname, prop, order, this);
  }

  @Override
  public IQuery getChildDocs(String docname, String prop, String order) throws XWikiException {
    return qp.getChildDocs(docname, prop, order, this);
  }

  @Override
  public IQuery getAttachment(String docname, String attachname, String order)
      throws XWikiException {
    return qp.getAttachment(docname, attachname, order, this);
  }

  @Override
  public IQuery getObjects(String docname, String oclass, String prop, String order)
      throws XWikiException {
    return qp.getObjects(docname, oclass, prop, order, this);
  }

  @Override
  public XWikiContext getContext() {
    return context;
  }

  @Override
  public XWikiStoreInterface getStore() {
    return qp.getStore(this);
  }

  @Override
  public IQuery xpath(String q) throws XWikiException {
    return xpath(q, true);
  }

  public IQuery xpath(String q, boolean checkRights) throws XWikiException {
    if (!checkRights && hasProgrammingRights()) {
      return qp.xpath(q, this);
    }

    if (log.isDebugEnabled()) {
      log.debug("create sec xpath query: " + q);
    }
    if (qp.isHibernate(this)) {
      try {
        return new SecHibernateQuery(qp.parse(q, Query.XPATH), this);
      } catch (InvalidQueryException e) {
        throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
            XWikiException.ERROR_XWIKI_UNKNOWN, "Invalid xpath query: " + q);
      }
    }
    if (qp.isJcr(this)) {
      return new SecJcrQuery(q, Query.XPATH, this);
    }
    return null;
  }

  @Override
  public IQuery ql(String q) throws XWikiException {
    return ql(q, true);
  }

  public IQuery ql(String q, boolean checkRights) throws XWikiException {
    if (!checkRights && hasProgrammingRights()) {
      return qp.ql(q, this);
    }

    if (log.isDebugEnabled()) {
      log.debug("create sec JCRSQL query: " + q);
    }
    if (qp.isHibernate(this)) {
      try {
        return new SecHibernateQuery(qp.parse(q, Query.SQL), this);
      } catch (InvalidQueryException e) {
        throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
            XWikiException.ERROR_XWIKI_UNKNOWN, "Invalid jcrsql query: " + q);
      }
    }
    if (qp.isJcr(this)) {
      return new SecJcrQuery(q, Query.SQL, this);
    }
    return null;
  }

  @Override
  public ValueFactory getValueFactory() {
    return qp.getValueFactory();
  }

  @Override
  public String makeQuery(XWikiQuery query) throws XWikiException {
    return qp.makeQuery(query);
  }
}
