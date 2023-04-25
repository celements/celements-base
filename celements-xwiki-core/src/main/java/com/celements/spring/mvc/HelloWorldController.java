package com.celements.spring.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloWorldController {

  @GetMapping("/helloworld")
  @ResponseBody
  public String helloWorld() {
    return "Hello World!";
  }

}
