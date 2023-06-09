package com.celements.query;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;

@ComponentRole
public interface IQueryExecutionServiceRole {

  @NotNull
  List<List<String>> executeReadSql(@NotNull String sql) throws XWikiException;

  @NotNull
  <T> List<List<T>> executeReadSql(@NotNull Class<T> type, @NotNull String sql)
      throws XWikiException;

  <T> void executeReadSql(@NotNull Class<T> type, @NotNull String sql,
      @NotNull Consumer<List<T>> action) throws XWikiException;

  int executeWriteSQL(@NotNull String sql) throws XWikiException;

  List<Integer> executeWriteSQLs(@NotNull List<String> sqls) throws XWikiException;

  int executeWriteHQL(@NotNull String hql, @NotNull Map<String, Object> binds)
      throws XWikiException;

  int executeWriteHQL(@NotNull String hql, @NotNull Map<String, Object> binds,
      @Nullable WikiReference wikiRef) throws XWikiException;

  @Nullable
  DocumentReference executeAndGetDocRef(@NotNull Query query) throws QueryException;

  @NotNull
  List<DocumentReference> executeAndGetDocRefs(@NotNull Query query) throws QueryException;

  boolean existsIndex(@NotNull WikiReference wikiRef, @NotNull String table,
      @NotNull String name) throws XWikiException;

}
