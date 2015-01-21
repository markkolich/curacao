package com.kolich.curacao.examples.controllers

import com.kolich.curacao.annotations.{RequestMapping, Controller}
import com.kolich.curacao.examples.entities.ReverseUserAgent

@Controller
class ScalaControllerExample {
  
  @RequestMapping("^\\/api\\/scala$")
  def helloWorld(ua:ReverseUserAgent): String = {
    "Hello from scala!" + "\n" + ua
  }

}
