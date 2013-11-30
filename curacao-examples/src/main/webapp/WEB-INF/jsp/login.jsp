<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
	<title>Curacao Example Login</title>
</head>
<body>
    <h2>Login</h2>
	<form method="post" action="login">
    	<p>
    		<label for="username">Username:</label>
    		<input type="text" id="username" name="username" />
    	</p>
    	<p>
    		<label for="password">Password:</label>
    		<input type="password" id="password" name="password" />
    	</p>
    	<p>
    		<input type="submit" value="Login" />
    	</p>
    </form>
</body>
</html>