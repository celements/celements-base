package com.celements.performance;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;

import com.google.common.base.Strings;

@Component
public class BenchmarkService implements BenchmarkRole {

  private static final String BENCH_LAST_TIME = "bench_lastTime";

  private static final String BENCH_START_TIME = "bench_startTime";

  private static final String CEL_BENCHMARK_VAL_PREFIX = "CEL_BENCHMARK_";

  private static final String CEL_BENCHMARK_OUT_STRINGS = CEL_BENCHMARK_VAL_PREFIX
      + "benchOutStrings";

  @Requirement
  Execution execution;

  private long getContextLongValue(String name) {
    return (long) execution.getContext().getProperty(CEL_BENCHMARK_VAL_PREFIX + name);
  }

  private void setContextLongValue(String name, long value) {
    execution.getContext().setProperty(CEL_BENCHMARK_VAL_PREFIX + name, value);
  }

  @SuppressWarnings("unchecked")
  private ArrayList<String> getBenchOutStringArray() {
    ArrayList<String> benchOutStringArray;
    if (execution.getContext().getProperty(CEL_BENCHMARK_OUT_STRINGS) == null) {
      benchOutStringArray = new ArrayList<>();
      execution.getContext().setProperty(CEL_BENCHMARK_OUT_STRINGS, benchOutStringArray);
    } else {
      benchOutStringArray = (ArrayList<String>) execution.getContext()
          .getProperty(CEL_BENCHMARK_OUT_STRINGS);
    }
    return benchOutStringArray;
  }

  @Override
  public void bench(String label) {
    checkArgument(!Strings.isNullOrEmpty(label));
    long currTime = new Date().getTime();
    long benchLastTime = getContextLongValue(BENCH_LAST_TIME);
    long benchStartTime = getContextLongValue(BENCH_START_TIME);
    double totalTime = (currTime - benchStartTime) / 1000.0;
    double time = (currTime - benchLastTime) / 1000.0;
    setContextLongValue(BENCH_LAST_TIME, benchLastTime);
    getBenchOutStringArray()
        .add("bench '" + label + "' &mdash; in " + time + "s &mdash; total " + totalTime
            + "s");
  }

  @Override
  public void startBench() {
    long benchStartTime = new Date().getTime();
    setContextLongValue(BENCH_START_TIME, benchStartTime);
    setContextLongValue(BENCH_LAST_TIME, benchStartTime);
  }

  @Override
  public String println(boolean visible) {
    String outStr = getBenchOutStringArray().stream()
        .collect(Collectors.joining("\n"));
    if (!visible) {
      outStr = "<!-- " + outStr + " -->";
    }
    return outStr;
  }

}
