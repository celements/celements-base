package com.celements.performance;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.script.service.ScriptService;

import com.celements.model.context.ModelContext;

@Component("bench")
public class BenchmarkScriptService implements ScriptService {

  @Requirement
  private ModelContext context;

  @Requirement
  private BenchmarkRole benchSrv;

  public String benchAndPrint(String label) {
    return benchAndPrint(label, false);
  }

  public void bench(String label) {
    benchSrv.bench(label);
  }

  public String benchAndPrint(String label, boolean visible) {
    benchSrv.bench(label);
    boolean requestVisible = context.getRequest().toJavaUtil()
        .map(request -> "true".equals(request.getParameter("showBenchmark")))
        .orElse(false);
    return benchSrv.println(visible || requestVisible);
  }

  public void startBench() {
    benchSrv.startBench();
  }

}
