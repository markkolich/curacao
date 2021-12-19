<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!doctype html>
<html>
<head>
	<title>Lanyon</title>
	<script src="//cdnjs.cloudflare.com/ajax/libs/json2/20121008/json2.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
    <link href="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css" rel="stylesheet">
    <link href="../static/css/lanyon/lanyon.css" rel="stylesheet"/>
</head>
<body>

    <!-- Target for toggling the sidebar `.sidebar-checkbox` is for regular
         styles, `#sidebar-checkbox` for behavior. -->
    <input type="checkbox" class="sidebar-checkbox" id="sidebar-checkbox">

    <!-- Toggleable sidebar -->
    <div class="sidebar" id="sidebar">
      <div class="sidebar-item">
        <p>Curacao!</p>
      </div>

      <nav class="sidebar-nav">
        <a class="sidebar-nav-item active" href="/curacao">Home</a>
        <a class="sidebar-nav-item" href="#">About</a>
        <a class="sidebar-nav-item" href="#">Contribute</a>
        <a class="sidebar-nav-item" href="https://github.com/markkolich/curacao">GitHub project</a>
        <span class="sidebar-nav-item">Currently v1.0.0</span>
      </nav>

      <div class="sidebar-item">
        <p>
          &copy; 2015. All rights reserved.
        </p>
      </div>
    </div>

    <div class="wrap">
        <div class="container">
            &nbsp;
        </div>
    </div>

    <label for="sidebar-checkbox" class="sidebar-toggle"></label>

    <!--
    <script type="text/javascript">
    var url = "/curacao/api/timeout";
    var timeout = 3000;
    var xhr = new XMLHttpRequest();
    xhr.ontimeout = function () {
        console.error("The request for " + url + " timed out.");
        console.error("ontimeout status: ", xhr.status);
        console.error("ontimeout statusText: ", xhr.statusText);
    };
    xhr.onload = function() {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                callback.apply(xhr, args);
            } else {
                console.error("status: ", xhr.status);
                console.error("statusText: ", xhr.statusText);
            }
        }
    };
    xhr.open("GET", url, true);
    xhr.timeout = timeout;
    xhr.send(null);
    </script>
    -->

</body>
</html>