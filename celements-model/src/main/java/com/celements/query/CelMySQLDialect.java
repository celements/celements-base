package com.celements.query;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.dialect.MySQLInnoDBDialect;

public class CelMySQLDialect extends MySQLInnoDBDialect {

  public CelMySQLDialect() {
    super();
    registerHibernateType(Types.LONGVARCHAR, Hibernate.TEXT.getName());
  }

  @Override
  public String getTableTypeString() {
    return " engine=InnoDB";
  }

}
