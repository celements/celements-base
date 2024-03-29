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
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.DBTreeListClass;
import com.xpn.xwiki.objects.classes.StringClass;

public class DBTreeListMetaClass extends DBListMetaClass {

  public DBTreeListMetaClass() {
    setPrettyName("Database Tree");
    setName(DBTreeListClass.class.getName());

    StringClass parentfield_class = new StringClass(this);
    parentfield_class.setName("parentField");
    parentfield_class.setPrettyName("Parent Field Name");
    parentfield_class.setSize(20);
    safeput("parentField", parentfield_class);
  }

  @Override
  public BaseCollection newObject(XWikiContext context) {
    return new DBTreeListClass();
  }
}
