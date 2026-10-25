package com.celements.performance;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface BenchmarkRole {

  void bench(@NotEmpty String label);

  void startBench();

  @NotNull
  String println(boolean visible);

}
