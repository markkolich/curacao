package com.kolich.curacao.examples.controllers

import com.kolich.curacao.annotations.{Controller}
import com.kolich.curacao.examples.entities.ReverseUserAgent
import com.kolich.curacao.annotations.methods.RequestMapping

@Controller
class ScalaControllerExample {
  
  @RequestMapping("^\\/api\\/scala$")
  def helloWorld(ua:ReverseUserAgent): String = {
    "Hello from scala!" + "\n" + ua
  }

}
