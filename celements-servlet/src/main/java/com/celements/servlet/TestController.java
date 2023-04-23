package com.celements.servlet;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestController {

  @GetMapping("/test")
  @ResponseBody
  public String testEndpoint() {
    return "Hello, this is a test spring mvc endpoint!";
  }
}
