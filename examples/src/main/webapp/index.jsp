<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
	<title>Curacao!</title>
	<script src="//cdnjs.cloudflare.com/ajax/libs/json2/20121008/json2.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
	<style type="text/css">
	li { margin-bottom: 10px; }
	</style>
</head>
<body>
	<h2>Oh hai!</h2>
	<h4>You're successfully running the Curacao example web-app.</h4>
	<h4>Would you like to?...</h4>
	<ul>
		<li><a href="javascript:void(0)" id="getjson">Issue a GET to load some JSON, as rendered by Google's GSON.</a></li>
		<li><a href="javascript:void(0)" id="postjson">Send some JSON as a POST body via AJAX, and unmarshall it on the server side using Google's GSON.</a></li>
		<li><a href="api/secure">Exercise Basic HTTP authentication.</a></li>
		<li><a href="api/jsp">Render a JSP, demonstrates dispatching the request context to a JSP.</a></li>
		<li><a href="api/chunked">Demonstrate a streaming "chunked" response sent to browser from Curacao.</a></li>
		<li><a href="api/future">Call a controller that returns a Future&lt;String&gt; after a random wait.</a></li>
		<li><a href="api/webservice">Call a controller that uses the AsyncHttpClient to make an async HTTP call to an external web-service.</a></li>
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
			$.post("json", JSON.stringify(obj), function(data, text, xhr) {
				if(xhr.status == 204) {
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