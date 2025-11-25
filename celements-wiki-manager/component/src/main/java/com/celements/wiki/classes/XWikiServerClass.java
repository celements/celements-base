package com.celements.wiki.classes;

import java.util.List;

import javax.inject.Singleton;

import org.springframework.stereotype.Component;
import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.EnumMarshaller;
import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.BooleanField;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.LargeStringField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.DisplayType;
import com.celements.model.classes.fields.list.ListOfUsersField;
import com.celements.model.classes.fields.list.single.EnumSingleListField;
import com.celements.model.classes.fields.list.single.StringSingleListField;
import com.celements.web.classes.oldcore.IOldCoreClassDef;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.user.api.XWikiUser;

@Singleton
@Component(XWikiServerClass.CLASS_DEF_HINT)
public class XWikiServerClass extends AbstractClassDefinition implements ClassDefinition {

  public static final String CLASS_SPACE = XWikiConstant.XWIKI_SPACE;
  public static final String CLASS_NAME = "XWikiServerClass";
  public static final String CLASS_FN = IOldCoreClassDef.CLASS_SPACE + "." + CLASS_NAME;
  public static final String CLASS_DEF_HINT = CLASS_FN;
  public static final ClassReference CLASS_REF = new ClassReference(CLASS_SPACE, CLASS_NAME);

  public enum Visibility {
    PUBLIC, PRIVATE
  }

  public enum State {
    ACTIVE, INACTIVE, LOCKED
  }

  public static final ClassField<String> FIELD_PRETTY_NAME = new StringField.Builder(
      CLASS_REF, "wikiprettyname")
          .prettyName("Wiki pretty name")
          .size(30)
          .build();

  public static final ClassField<List<XWikiUser>> FIELD_OWNER = new ListOfUsersField.Builder(
      CLASS_REF, "owner")
          .prettyName("Owner")
          .displayType(DisplayType.select)
          .multiSelect(false)
          .size(5)
          .usesList(true)
          .build();

  public static final ClassField<String> FIELD_DESCRIPTION = new LargeStringField.Builder(
      CLASS_REF, "description")
          .rows(5)
          .prettyName("Description")
          .size(40)
          .build();

  public static final ClassField<String> FIELD_SERVER = new StringField.Builder(
      CLASS_REF, "server")
          .prettyName("Server")
          .size(30)
          .build();

  public static final ClassField<Visibility> FIELD_VISIBILITY = new EnumSingleListField.Builder<>(
      CLASS_REF, "visibility", new EnumMarshaller<>(Visibility.class, e -> e.name().toLowerCase()))
          .prettyName("Visibility")
          .displayType(DisplayType.select)
          .size(1)
          .build();

  public static final ClassField<State> FIELD_STATE = new EnumSingleListField.Builder<>(
      CLASS_REF, "state", new EnumMarshaller<>(State.class, e -> e.name().toLowerCase()))
          .prettyName("State")
          .prettyName("State")
          .displayType(DisplayType.select)
          .size(1)
          .build();

  public static final ClassField<String> FIELD_LANGUAGE = new StringSingleListField.Builder(
      CLASS_REF, "language")
          .prettyName("Language")
          .displayType(DisplayType.select)
          .values(List.of("en", "de", "fr", "it"))
          .size(1)
          .build();

  public static final ClassField<Boolean> FIELD_SECURE = new BooleanField.Builder(
      CLASS_REF, "secure")
          .prettyName("Secure")
          .defaultValue(0)
          .dictionaryKey("checkbox")
          .displayFormType("select")
          .build();

  public static final ClassField<String> FIELD_HOMEPAGE = new StringField.Builder(
      CLASS_REF, "homepage")
          .prettyName("Home page")
          .size(30)
          .build();

  public static final ClassField<Boolean> FIELD_IS_TEMPLATE = new BooleanField.Builder(
      CLASS_REF, "iswikitemplate")
          .prettyName("Template")
          .defaultValue(0)
          .dictionaryKey("checkbox")
          .displayFormType("select")
          .build();

  public static final ClassField<Boolean> FIELD_OICD_ACTIVE = new BooleanField.Builder(
      CLASS_REF, "oicd-active")
          .prettyName("OpenID Connect active")
          .defaultValue(0)
          .dictionaryKey("checkbox")
          .displayFormType("select")
          .build();

  public XWikiServerClass() {
    super(CLASS_REF);
  }

  @Override
  public boolean isInternalMapping() {
    return false;
  }

}
