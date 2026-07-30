package com.xpn.xwiki.doc;

import static com.google.common.base.Preconditions.*;
import static java.util.Objects.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.NumberProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;

public record CelProperty(
    String name,
    Type type,
    Value value) {

  public CelProperty {
    requireNonNull(name);
    requireNonNull(type);
    requireNonNull(value);
  }

  private CelProperty(BaseProperty property) {
    this(requireNonNull(property).getName(), Type.from(property), toCelValue(property));
  }

  public static CelProperty from(BaseProperty property) {
    return new CelProperty(property);
  }

  public String getName() {
    return name();
  }

  public Type getType() {
    return type();
  }

  public Object getValue() {
    if (value instanceof NullValue()) {
      return null;
    } else if (value instanceof StringValue(String stringValue)) {
      return stringValue;
    } else if (value instanceof DateValue(Instant dateValue)) {
      return Optional.ofNullable(dateValue).map(Date::from).orElse(null);
    } else if (value instanceof ListValue(List<String> listValue)) {
      return new ArrayList<>(listValue);
    } else if (value instanceof NumberValue(String numberValue)) {
      return Optional.ofNullable(numberValue).map(valueString -> switch (type) {
        case INTEGER -> Integer.valueOf(valueString);
        case LONG -> Long.valueOf(valueString);
        case FLOAT -> Float.valueOf(valueString);
        case DOUBLE -> Double.valueOf(valueString);
        default -> throw new IllegalStateException("unsupported number XProperty " + type);
      }).orElse(null);
    }
    throw new IllegalStateException("unsupported XProperty value " + value);
  }

  public String getStringValue() {
    return value instanceof StringValue string ? nullToEmpty(string.value())
        : value instanceof NumberValue number ? nullToEmpty(number.value())
            : "";
  }

  public int getIntValue() {
    try {
      return value instanceof NumberValue number && number.value() != null
          ? Integer.parseInt(number.value())
          : 0;
    } catch (NumberFormatException exc) {
      return 0;
    }
  }

  public Instant getDateValue() {
    return value instanceof DateValue date ? date.value() : null;
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  @SuppressWarnings("unchecked")
  private static Value toCelValue(BaseProperty property) {
    Object value = property.getValue();
    if (value == null) {
      return new NullValue();
    } else if (property instanceof DateProperty) {
      return new DateValue(((Date) value).toInstant());
    } else if (property instanceof ListProperty) {
      return new ListValue((List<String>) value);
    } else if (property instanceof NumberProperty) {
      return new NumberValue(value.toString());
    } else if (value instanceof String string) {
      return new StringValue(string);
    }
    throw new IllegalStateException("unsupported XProperty " + property.getClass().getName());
  }

  public sealed interface Value permits NullValue, StringValue, NumberValue, DateValue, ListValue {}

  public record NullValue() implements Value {}

  public record StringValue(String value) implements Value {}

  public record NumberValue(String value) implements Value {}

  public record DateValue(Instant value) implements Value {}

  public record ListValue(List<String> value) implements Value {

    public ListValue {
      value = List.copyOf(value);
    }
  }

  public enum Type {
    BASE(BaseProperty.class),
    BASE_STRING(BaseStringProperty.class),
    STRING(StringProperty.class),
    LARGE_STRING(LargeStringProperty.class),
    INTEGER(IntegerProperty.class),
    LONG(LongProperty.class),
    FLOAT(FloatProperty.class),
    DOUBLE(DoubleProperty.class),
    DATE(DateProperty.class),
    LIST(ListProperty.class),
    STRING_LIST(StringListProperty.class),
    DB_STRING_LIST(DBStringListProperty.class);

    private final Class<? extends BaseProperty> propertyClass;

    Type(Class<? extends BaseProperty> propertyClass) {
      this.propertyClass = propertyClass;
    }

    private static Type from(BaseProperty property) {
      Optional<Type> type = Arrays.stream(values())
          .filter(candidate -> candidate.propertyClass.equals(property.getClass()))
          .findFirst();
      checkArgument(type.isPresent(),
          "unsupported XProperty %s", property.getClass().getName());
      return type.get();
    }
  }
}
