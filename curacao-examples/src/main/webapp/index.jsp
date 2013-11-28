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
    <h2>Oh hai!</h2>
	<h4>You're successfully running the Curacao example web-app.</h4>
	<h4>Here's some examples:</h4>
	<ul>
		<li><a href="javascript:void(0)" id="getjson">Issue a GET request to load some JSON, as rendered by Google's GSON.</a></li>
		<li class="separator"><a href="javascript:void(0)" id="postjson">Send some JSON in a POST body via AJAX, and unmarshall it on the server side using Google's GSON.</a></li>
		<li class="separator"><a href="api/secure">Exercise Basic HTTP authentication.</a></li>
		<li class="separator"><a href="api/jsp">Render a JSP, demonstrates dispatching the request context to a JSP.</a></li>
		<li><a href="api/chunked">Demonstrate a streaming "chunked" response sent to browser from Curacao.</a></li>
		<li><a href="api/future">Call a controller that returns a Future&lt;String&gt; after a random wait.</a></li>
		<li><a href="api/webservice">Call a controller that uses the AsyncHttpClient to make an async HTTP call to an external web-service.</a></li>
		<li>
			<p>Send some data via a POST body</p>
			<form method="post" action="api/postbody">
				<input type="text" name="data">
				<input type="text" name="moredata">
				<input type="text" name="moredata">
				<input type="submit" value="Submit">
			</form>
		</li>
	</ul>
	<script>
	$(function(e) {
		$("#getjson").unbind().click(function(e) {
			$.get("api/json", function(data, text, xhr) {
				alert(JSON.stringify(data));
			});
			e.preventDefault();
		});
		$("#postjson").unbind().click(function(e) {
			var obj = {
				"foo": navigator.userAgent,
				"bar": new Date().getTime()
			};
			$.post("api/json", JSON.stringify(obj), function(data, text, xhr) {
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