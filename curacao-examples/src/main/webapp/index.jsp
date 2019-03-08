<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
	<title>Curacao</title>
	<script src="//cdnjs.cloudflare.com/ajax/libs/json2/20121008/json2.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
	<style type="text/css">
	li { margin-bottom: 10px; }
	li.separator { margin-bottom: 25px; }
	</style>
</head>
<body>
    <h2>Curacao!</h2>
	<h4>You're successfully running the Curacao example web-app.</h4>
	<h4>This example web-app is used for testing, and demonstrating how Curacao works.</h4>
	<h4>Here's some examples:</h4>
	<ul>
		<li><a href="api/json/gson" class="getjson">Issue a GET request to load some JSON, as rendered by Google's GSON.</a></li>
		<li class="separator"><a href="api/json/gson" class="postjson">Send some JSON in a POST body via AJAX, and unmarshall it on the server side using Google's GSON.</a></li>
		<li><a href="api/json/jackson" class="getjson">Issue a GET request to load some JSON, as rendered by Jackson.</a></li>
		<li class="separator"><a href="api/json/jackson" class="postjson">Send some JSON in a POST body via AJAX, and unmarshall it on the server side using Jackson.</a></li>
		<li class="separator"><a href="api/secure">Exercise Basic HTTP authentication.</a></li>

		<li class="separator"><a href="api/login">Try a session protected login to a secured path.</a></li>

		<li class="separator"><a href="api/jsp">Render a JSP, demonstrates dispatching the request context to a JSP.</a></li>

		<li class="separator"><a href="api/abstract">Invoke abstract method in controller parent class.</a></li>

		<li class="separator"><a href="api/chunked">Demonstrate a streaming "chunked" response sent to browser from Curacao.</a></li>

		<li class="separator"><a href="api/reverse">See your User-Agent in reverse.</a></li>

        <li class="separator"><a href="api/lanyon">Render a JSP that loads static content (CSS/JS) as served by Curacao.</a></li>

        <li class="separator"><a href="api/timeout">Force an AsyncContext timeout (after 10-seconds) and see error handling.</a></li>

		<li class="separator"><a href="api/queryparameters?string=foobar&int=12121&long=98247378293747829&char=m&boolean=1">Demonstrate query parameter mappers.</a></li>
        <li class="separator"><a href="api/pathparameters/foobar/12121/98247378293747829/m/yes">Demonstrate path parameter mappers.</a></li>

        <li class="separator"><a href="api/annotation">Read values attached to custom annotations.</a></li>

		<li>
			<p>Send some data via a POST body</p>
			<form method="post" action="api/postbody">
				<label for="data">Data:</label><input type="text" name="data" id="data">
				<label for="moredata">More data:</label><input type="text" name="moredata" id="moredata">
				<label for="moredata1">More data:</label><input type="text" name="moredata" id="moredata1">
				<input type="submit" value="Submit">
			</form>
		</li>
	</ul>
	<script>
	$(function(e) {
		$("a.getjson").unbind().click(function(e) {
		    var href = $(this).attr("href");
			$.get(href, function(data, text, xhr) {
				alert(JSON.stringify(data));
			});
			e.preventDefault();
		});
		$("a.postjson").unbind().click(function(e) {
		    var href = $(this).attr("href");
			var obj = {
				"foo": navigator.userAgent,
				"bar": new Date().getTime()
			};
			$.post(href, JSON.stringify(obj), function(data, text, xhr) {
				if(xhr.status == 200) {
					alert("Success!\n\nServer says:\nHTTP " + xhr.status);
				} else {
					alert("Oops, something went wrong.");
				}
			});
            e.preventDefault();
		});
	});
	</script>
</body>
</html>