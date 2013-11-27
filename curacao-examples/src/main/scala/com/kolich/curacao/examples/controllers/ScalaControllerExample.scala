package com.kolich.curacao.examples.controllers

import com.kolich.curacao.annotations.Controller
import com.kolich.curacao.annotations.methods.GET
import com.kolich.curacao.annotations.parameters.RequestUri
import com.kolich.curacao.annotations.parameters.convenience.UserAgent
import com.kolich.curacao.examples.entities.ReverseUserAgent

@Controller
class ScalaControllerExample {
  
  @GET("/api/scala")
  def helloWorld(ua:ReverseUserAgent): String = {
    "Hello from scala!" + "\n" + ua
  }

}
