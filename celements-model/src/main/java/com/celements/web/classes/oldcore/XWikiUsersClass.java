package com.celements.web.classes.oldcore;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.BooleanField;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.single.EnumSingleListField;

@Singleton
@Component(XWikiUsersClass.CLASS_DEF_HINT)
public class XWikiUsersClass extends AbstractClassDefinition implements IOldCoreClassDef {

  public enum Type {
    Simple, Advanced;
  }

  public static final String CLASS_NAME = "XWikiUsers";
  public static final String CLASS_FN = IOldCoreClassDef.CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;
  public static final ClassReference CLASS_REF = new ClassReference(CLASS_SPACE, CLASS_NAME);

  public static final ClassField<String> FIELD_FIRST_NAME = new StringField.Builder(CLASS_REF,
      "first_name").prettyName("First Name").size(30).build();

  public static final ClassField<String> FIELD_LAST_NAME = new StringField.Builder(CLASS_REF,
      "last_name").prettyName("Last Name").size(30).build();

  public static final ClassField<String> FIELD_EMAIL = new StringField.Builder(CLASS_REF,
      "email").prettyName("e-Mail").size(30).build();

  public static final ClassField<String> FIELD_PASSWORD = new StringField.Builder(CLASS_REF,
      "password").prettyName("Password").size(10).build();

  public static final ClassField<String> FIELD_VALID_KEY = new StringField.Builder(CLASS_REF,
      "validkey").prettyName("Validation Key").size(10).build();

  public static final ClassField<Boolean> FIELD_ACTIVE = new BooleanField.Builder(CLASS_REF,
      "active").prettyName("Active").displayType("active").build();

  public static final ClassField<Boolean> FIELD_SUSPENDED = new BooleanField.Builder(CLASS_REF,
      "suspended").prettyName("Suspended").displayType("suspended").defaultValue(0).build();

  public static final ClassField<Type> FIELD_TYPE = new EnumSingleListField.Builder<>(CLASS_REF,
      "usertype", Type.class).prettyName("User type").build();

  // XXX class is incomplete, extend if needed:
  // addTextField("default_language", "Default Language", 30);
  // addTextField("company", "Company", 30);
  // addTextField("blog", "Blog", 60);
  // addTextField("blogfeed", "Blog Feed", 60);
  // addTextAreaField("comment", "Comment", 40, 5);
  // addStaticListField("imtype", "IM Type", "---|AIM|Yahoo|Jabber|MSN|Skype|ICQ");
  // addTextField("imaccount", "imaccount", 30);
  // addStaticListField("editor", "Default Editor", "---|Text|Wysiwyg");
  // addBooleanField("accessibility", "Enable extra accessibility features", "yesno");
  // addTextField("skin", "skin", 30);
  // addStaticListField("pageWidth", "Preferred page width", "default|640|800|1024|1280|1600");
  // addTextField("avatar", "Avatar", 30);
  // addTextField("phone", "Phone", 30);
  // addTextAreaField("address", "Address", 40, 3);

  // additional Celements field
  public static final ClassField<Boolean> FIELD_FORCE_PWD_CHANGE = new BooleanField.Builder(
      CLASS_REF, "force_pwd_change").prettyName("force_pwd_change").displayType("yesno").build();

  // additional Celements field
  public static final ClassField<String> FIELD_ADMIN_LANG = new StringField.Builder(CLASS_REF,
      "admin_language").prettyName("User Edit-Interface Language").size(4).build();

  public XWikiUsersClass() {
    super(CLASS_REF);
  }

  @Override
  public boolean isInternalMapping() {
    return false;
  }

}
